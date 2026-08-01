package dev.inboxpilot.infrastructure.gmail;

import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.application.port.MessageSourceException;
import dev.inboxpilot.application.inventory.InventoryProgressTracker;
import dev.inboxpilot.application.port.InventoryProgressReporter;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.infrastructure.config.BatchingProperties;
import dev.inboxpilot.infrastructure.config.InboxPilotProperties;
import java.io.IOException;
import java.time.Instant;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Gmail adapter for the {@link MessageSource} port.
 *
 * <p>This is the one place allowed to know about Google client types: it calls
 * the Gmail API and translates the result into
 * {@link dev.inboxpilot.domain.message.MailMessage} values, so nothing further
 * in the application ever sees a Gmail type (ADR 0001, enforced by
 * {@code ArchitectureTest}).
 *
 * <p>Requests use Gmail's metadata format, are split according to the configured
 * batch size, and preserve successful items when another item fails.
 */
@Component
public class GmailMessageSource implements MessageSource {

    private static final Logger logger = LoggerFactory.getLogger(GmailMessageSource.class);

    private static final String QUERY_PREFIX = "after:";
    private static final String METADATA_FAILURE = "Gmail message metadata could not be fetched: ";
    private static final String REPEATED_PAGE_TOKEN =
            "Gmail returned a repeated page token while listing message ids";
    private static final String FETCH_LOG_MESSAGE = "Gmail fetch requested for messages since {}";
    private static final String DEGRADED_LOG_MESSAGE =
            "Gmail metadata fetch degraded for message {}: {}";
    private static final String RETRY_LOG_MESSAGE =
            "Retrying {} Gmail metadata requests after {} (attempt {})";
    private static final Comparator<MailMessage> OLDEST_FIRST =
            Comparator.comparing(MailMessage::receivedAt);

    private final GmailApiClient client;
    private final GmailRetryPolicy retryPolicy;
    private final RetrySleeper sleeper;
    private final InventoryProgressReporter progressReporter;
    private final Clock clock;
    private final int batchSize;
    private final GmailQuotaRateLimiter quotaRateLimiter;

    @Autowired
    public GmailMessageSource(
            GmailApiClient client,
            InboxPilotProperties properties,
            InventoryProgressReporter progressReporter) {
        this(client, properties.batching(),
                duration -> Thread.sleep(duration.toMillis()), progressReporter, Clock.systemUTC());
    }

    GmailMessageSource(GmailApiClient client, BatchingProperties batchingProperties) {
        this(client, batchingProperties, duration -> Thread.sleep(duration.toMillis()));
    }

    GmailMessageSource(
            GmailApiClient client,
            BatchingProperties batchingProperties,
            RetrySleeper sleeper) {
        this(client, batchingProperties, sleeper, progress -> { }, Clock.systemUTC());
    }

    GmailMessageSource(
            GmailApiClient client,
            BatchingProperties batchingProperties,
            RetrySleeper sleeper,
            InventoryProgressReporter progressReporter,
            Clock clock) {
        this.client = client;
        this.batchSize = batchingProperties.batchSize();
        this.retryPolicy = new GmailRetryPolicy(
                batchingProperties.maxRetries(),
                batchingProperties.initialBackoff(),
                batchingProperties.maxBackoff());
        this.sleeper = sleeper;
        this.quotaRateLimiter = new GmailQuotaRateLimiter(sleeper);
        this.progressReporter = progressReporter;
        this.clock = clock;
    }

    @Override
    public List<MailMessage> fetchSince(Instant since) {
        return fetchSince(since, Integer.MAX_VALUE);
    }

    @Override
    public List<MailMessage> fetchSince(Instant since, int maximumMessages) {
        logger.debug(FETCH_LOG_MESSAGE, since);
        try {
            List<String> messageIds = listAllMessageIds(
                    QUERY_PREFIX + since.getEpochSecond(), maximumMessages);
            InventoryProgressTracker progress = new InventoryProgressTracker(
                    messageIds.size(), progressReporter, clock);
            return fetchBatches(messageIds, progress).stream().sorted(OLDEST_FIRST).toList();
        } catch (IOException exception) {
            throw new MessageSourceException(METADATA_FAILURE + exception.getMessage(), exception);
        }
    }

    private List<String> listAllMessageIds(String query, int maximumMessages) throws IOException {
        List<String> messageIds = new ArrayList<>();
        HashSet<String> seenPageTokens = new HashSet<>();
        String pageToken = null;
        do {
            quotaRateLimiter.beforeMessageList();
            GmailMessagePage page = client.listMessageIds(query, pageToken);
            int remaining = maximumMessages - messageIds.size();
            messageIds.addAll(page.messageIds().stream().limit(remaining).toList());
            pageToken = page.nextPageToken();
            if (pageToken != null && !seenPageTokens.add(pageToken)) {
                throw new IOException(REPEATED_PAGE_TOKEN);
            }
        } while (pageToken != null && messageIds.size() < maximumMessages);
        return messageIds;
    }

    private List<MailMessage> fetchBatches(
            List<String> messageIds,
            InventoryProgressTracker progress) throws IOException {
        List<MailMessage> messages = new ArrayList<>();
        int start = 0;
        while (start < messageIds.size()) {
            int end = Math.min(start + batchSize, messageIds.size());
            fetchWithRetries(messageIds.subList(start, end), messages, progress);
            progress.recordProcessed(end - start);
            start = end;
        }
        return List.copyOf(messages);
    }

    private void fetchWithRetries(
            List<String> messageIds,
            List<MailMessage> messages,
            InventoryProgressTracker progress)
            throws IOException {
        List<String> pending = List.copyOf(messageIds);
        int retriesCompleted = 0;
        while (!pending.isEmpty()) {
            quotaRateLimiter.beforeMessageGetBatch(pending.size());
            GmailMetadataBatch batch = client.getMessageMetadata(pending);
            batch.messages().stream().map(GmailMessageSource::toMailMessage).forEach(messages::add);
            pending = retryableIds(batch.failures(), retriesCompleted);
            if (!pending.isEmpty()) {
                retriesCompleted++;
                pauseBeforeRetry(pending.size(), retriesCompleted, progress);
            }
        }
    }

    private List<String> retryableIds(List<GmailMetadataFailure> failures, int retriesCompleted) {
        failures.stream()
                .filter(failure -> !failure.retryable() || !retryPolicy.canRetry(retriesCompleted))
                .forEach(this::reportFailure);
        if (!retryPolicy.canRetry(retriesCompleted)) {
            return List.of();
        }
        return failures.stream()
                .filter(GmailMetadataFailure::retryable)
                .map(GmailMetadataFailure::messageId)
                .toList();
    }

    private void pauseBeforeRetry(
            int messageCount,
            int retryNumber,
            InventoryProgressTracker progress) throws IOException {
        java.time.Duration delay = retryPolicy.delayBeforeRetry(retryNumber);
        progress.recordRetry();
        logger.warn(RETRY_LOG_MESSAGE, messageCount, delay, retryNumber);
        try {
            sleeper.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(exception);
        }
    }

    private void reportFailure(GmailMetadataFailure failure) {
        logger.warn(DEGRADED_LOG_MESSAGE,
                failure.messageId(), failure.reason());
    }

    private static MailMessage toMailMessage(GmailMessageMetadata metadata) {
        return new MailMessage(
                new MessageId(metadata.id()),
                new EmailAddress(metadata.sender()),
                metadata.subject(),
                metadata.receivedAt(),
                metadata.labelIds());
    }
}
