package dev.inboxpilot.infrastructure.gmail;

import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.application.port.MessageSourceException;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.infrastructure.config.BatchingProperties;
import dev.inboxpilot.infrastructure.config.InboxPilotProperties;
import java.io.IOException;
import java.time.Instant;
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
    private static final Comparator<MailMessage> OLDEST_FIRST =
            Comparator.comparing(MailMessage::receivedAt);

    private final GmailApiClient client;
    private final int batchSize;

    @Autowired
    public GmailMessageSource(GmailApiClient client, InboxPilotProperties properties) {
        this(client, properties.batching());
    }

    GmailMessageSource(GmailApiClient client, BatchingProperties batchingProperties) {
        this.client = client;
        this.batchSize = batchingProperties.batchSize();
    }

    @Override
    public List<MailMessage> fetchSince(Instant since) {
        logger.debug(FETCH_LOG_MESSAGE, since);
        try {
            List<String> messageIds = listAllMessageIds(QUERY_PREFIX + since.getEpochSecond());
            return fetchBatches(messageIds).stream().sorted(OLDEST_FIRST).toList();
        } catch (IOException exception) {
            throw new MessageSourceException(METADATA_FAILURE + exception.getMessage(), exception);
        }
    }

    private List<String> listAllMessageIds(String query) throws IOException {
        List<String> messageIds = new ArrayList<>();
        HashSet<String> seenPageTokens = new HashSet<>();
        String pageToken = null;
        do {
            GmailMessagePage page = client.listMessageIds(query, pageToken);
            messageIds.addAll(page.messageIds());
            pageToken = page.nextPageToken();
            if (pageToken != null && !seenPageTokens.add(pageToken)) {
                throw new IOException(REPEATED_PAGE_TOKEN);
            }
        } while (pageToken != null);
        return messageIds;
    }

    private List<MailMessage> fetchBatches(List<String> messageIds) throws IOException {
        List<MailMessage> messages = new ArrayList<>();
        for (int start = 0; start < messageIds.size(); start += batchSize) {
            int end = Math.min(start + batchSize, messageIds.size());
            GmailMetadataBatch batch = client.getMessageMetadata(messageIds.subList(start, end));
            batch.failures().forEach(this::reportFailure);
            batch.messages().stream().map(GmailMessageSource::toMailMessage).forEach(messages::add);
        }
        return List.copyOf(messages);
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
