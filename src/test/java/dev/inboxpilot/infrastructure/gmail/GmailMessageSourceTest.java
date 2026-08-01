package dev.inboxpilot.infrastructure.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.application.model.InventoryProgress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.infrastructure.config.BatchingProperties;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GmailMessageSource")
class GmailMessageSourceTest {

    private static final Instant SINCE = Instant.parse("2026-08-01T10:15:30Z");
    private static final Instant RECEIVED_AT = Instant.parse("2026-08-01T10:16:30Z");
    private static final String FIRST_ID = "message-1";
    private static final String SECOND_ID = "message-2";
    private static final String THIRD_ID = "message-3";
    private static final String SENDER = "sender@example.com";
    private static final String SUBJECT = "Status update";
    private static final String LABEL = "INBOX";
    private static final String FAILURE_REASON = "rate limited";
    private static final String EXPECTED_QUERY = "after:" + 1785579330L;
    private static final int BATCH_SIZE = 2;
    private static final int MAX_RETRIES = 0;
    private static final int ONE_RETRY = 1;
    private static final Duration NO_BACKOFF = Duration.ZERO;
    private static final int ALL_MESSAGES = 3;

    @Test
    @DisplayName("retrieves metadata in configured batches")
    void retrievesMetadataInConfiguredBatches() {
        StubGmailApiClient client = clientWithIds(FIRST_ID, SECOND_ID, THIRD_ID);
        client.results.add(successfulBatch(FIRST_ID, SECOND_ID));
        client.results.add(successfulBatch(THIRD_ID));

        List<MailMessage> messages = source(client).fetchSince(SINCE);

        assertThat(client.requestedBatches)
                .containsExactly(List.of(FIRST_ID, SECOND_ID), List.of(THIRD_ID));
        assertThat(client.query).isEqualTo(EXPECTED_QUERY);
        assertThat(messages).extracting(message -> message.id().value())
                .containsExactly(FIRST_ID, SECOND_ID, THIRD_ID);
    }

    @Test
    @DisplayName("stops metadata retrieval at the configured inventory ceiling")
    void respectsMaximumMessages() {
        StubGmailApiClient client = clientWithIds(FIRST_ID, SECOND_ID, THIRD_ID);
        client.results.add(successfulBatch(FIRST_ID, SECOND_ID));

        List<MailMessage> messages = source(client).fetchSince(SINCE, 2);

        assertThat(client.requestedBatches).containsExactly(List.of(FIRST_ID, SECOND_ID));
        assertThat(messages).extracting(message -> message.id().value())
                .containsExactly(FIRST_ID, SECOND_ID);
    }

    @Test
    @DisplayName("keeps successful messages when one batch item fails")
    void keepsSuccessfulMessagesAfterPartialFailure() {
        StubGmailApiClient client = clientWithIds(FIRST_ID, SECOND_ID);
        client.results.add(new GmailMetadataBatch(
                List.of(metadata(FIRST_ID)),
                List.of(permanentFailure(SECOND_ID))));

        List<MailMessage> messages = source(client).fetchSince(SINCE);

        assertThat(messages).extracting(message -> message.id().value())
                .containsExactly(FIRST_ID);
    }

    @Test
    @DisplayName("skips malformed sender metadata without losing valid batch items")
    void skipsMalformedSenderMetadata() {
        StubGmailApiClient client = clientWithIds(FIRST_ID, SECOND_ID);
        GmailMessageMetadata malformed =
                new GmailMessageMetadata(FIRST_ID, "booking.com", SUBJECT, RECEIVED_AT, List.of());
        client.results.add(new GmailMetadataBatch(
                List.of(malformed, metadata(SECOND_ID)), List.of()));

        List<MailMessage> messages = source(client).fetchSince(SINCE);

        assertThat(messages).extracting(message -> message.id().value())
                .containsExactly(SECOND_ID);
    }

    @Test
    @DisplayName("retries only failed messages after throttling")
    void retriesFailedMessagesAfterThrottling() {
        StubGmailApiClient client = clientWithIds(FIRST_ID, SECOND_ID);
        client.results.add(new GmailMetadataBatch(
                List.of(metadata(FIRST_ID)), List.of(throttledFailure(SECOND_ID))));
        client.results.add(successfulBatch(SECOND_ID));

        List<MailMessage> messages = source(client, ONE_RETRY).fetchSince(SINCE);

        assertThat(client.requestedBatches)
                .containsExactly(List.of(FIRST_ID, SECOND_ID), List.of(SECOND_ID));
        assertThat(messages).extracting(message -> message.id().value())
                .containsExactly(FIRST_ID, SECOND_ID);
    }

    @Test
    @DisplayName("keeps efficient batch sizes after throttling while the quota limiter paces calls")
    void keepsBatchSizeAfterThrottling() {
        StubGmailApiClient client = clientWithIds(FIRST_ID, SECOND_ID, THIRD_ID);
        client.results.add(new GmailMetadataBatch(
                List.of(metadata(FIRST_ID)), List.of(throttledFailure(SECOND_ID))));
        client.results.add(successfulBatch(SECOND_ID));
        client.results.add(successfulBatch(THIRD_ID));

        source(client, ONE_RETRY).fetchSince(SINCE);

        assertThat(client.requestedBatches)
                .containsExactly(List.of(FIRST_ID, SECOND_ID), List.of(SECOND_ID), List.of(THIRD_ID));
    }

    @Test
    @DisplayName("reports completed and remaining message counts")
    void reportsInventoryProgress() {
        StubGmailApiClient client = clientWithIds(FIRST_ID, SECOND_ID, THIRD_ID);
        client.results.add(successfulBatch(FIRST_ID, SECOND_ID));
        client.results.add(successfulBatch(THIRD_ID));
        List<InventoryProgress> reports = new ArrayList<>();
        BatchingProperties properties = new BatchingProperties(
                BATCH_SIZE, MAX_RETRIES, NO_BACKOFF, NO_BACKOFF);
        GmailMessageSource source = new GmailMessageSource(
                client, properties, duration -> { }, reports::add,
                Clock.fixed(SINCE, ZoneOffset.UTC));

        source.fetchSince(SINCE);

        assertThat(reports.getLast().processedMessages()).isEqualTo(ALL_MESSAGES);
        assertThat(reports.getLast().remainingMessages()).isZero();
    }

    @Test
    @DisplayName("satisfies the provider-independent port")
    void satisfiesThePort() {
        assertThat(source(clientWithIds())).isInstanceOf(MessageSource.class);
    }

    private static GmailMessageSource source(StubGmailApiClient client) {
        return source(client, MAX_RETRIES);
    }

    private static GmailMessageSource source(StubGmailApiClient client, int maxRetries) {
        BatchingProperties properties = new BatchingProperties(
                BATCH_SIZE, maxRetries, NO_BACKOFF, NO_BACKOFF);
        return new GmailMessageSource(client, properties, duration -> { });
    }

    private static StubGmailApiClient clientWithIds(String... ids) {
        StubGmailApiClient client = new StubGmailApiClient();
        client.messageIds = List.of(ids);
        return client;
    }

    private static GmailMetadataBatch successfulBatch(String... ids) {
        return new GmailMetadataBatch(
                List.of(ids).stream().map(GmailMessageSourceTest::metadata).toList(),
                List.of());
    }

    private static GmailMessageMetadata metadata(String id) {
        return new GmailMessageMetadata(id, SENDER, SUBJECT, RECEIVED_AT, List.of(LABEL));
    }

    private static GmailMetadataFailure permanentFailure(String id) {
        return new GmailMetadataFailure(id, FAILURE_REASON, false, false);
    }

    private static GmailMetadataFailure throttledFailure(String id) {
        return new GmailMetadataFailure(id, FAILURE_REASON, true, true);
    }

    private static final class StubGmailApiClient implements GmailApiClient {
        private List<String> messageIds = List.of();
        private final List<GmailMetadataBatch> results = new ArrayList<>();
        private final List<List<String>> requestedBatches = new ArrayList<>();
        private String query;
        private int resultIndex;

        @Override
        public List<GmailLabel> listLabels() {
            return List.of();
        }

        @Override
        public GmailMessagePage listMessageIds(String requestedQuery, String pageToken) {
            query = requestedQuery;
            return new GmailMessagePage(messageIds, null);
        }

        @Override
        public GmailMetadataBatch getMessageMetadata(List<String> ids) throws IOException {
            requestedBatches.add(List.copyOf(ids));
            return results.get(resultIndex++);
        }

        @Override
        public void batchModifyLabels(
                List<String> ids, List<String> addLabelIds, List<String> removeLabelIds) {}
    }
}
