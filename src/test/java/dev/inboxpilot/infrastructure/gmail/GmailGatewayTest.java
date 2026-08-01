package dev.inboxpilot.infrastructure.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import dev.inboxpilot.application.model.MailboxLabel;
import dev.inboxpilot.application.port.MailboxGatewayException;
import dev.inboxpilot.domain.message.MessageId;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GmailGateway")
class GmailGatewayTest {

    private static final String QUERY = "after:2026/07/01 -in:trash";
    private static final String FIRST_PAGE_TOKEN = "next-page";
    private static final String FIRST_MESSAGE_ID = "message-one";
    private static final String SECOND_MESSAGE_ID = "message-two";
    private static final String LABEL_ID = "Label_42";
    private static final String LABEL_NAME = "Receipts";
    private static final String API_FAILURE = "service unavailable";
    private static final String REPEATED_PAGE_TOKEN_MESSAGE = "repeated page token";

    @Test
    @DisplayName("lists provider-independent labels")
    void listsLabels() {
        StubGmailApiClient client = new StubGmailApiClient();
        client.labels = List.of(new GmailLabel(LABEL_ID, LABEL_NAME));

        assertThat(new GmailGateway(client).listLabels())
                .containsExactly(new MailboxLabel(LABEL_ID, LABEL_NAME));
    }

    @Test
    @DisplayName("passes the search query and follows every message page")
    void listsEveryMessagePage() {
        StubGmailApiClient client = new StubGmailApiClient();
        client.pages = List.of(
                new GmailMessagePage(List.of(FIRST_MESSAGE_ID), FIRST_PAGE_TOKEN),
                new GmailMessagePage(List.of(SECOND_MESSAGE_ID), null));

        assertThat(new GmailGateway(client).listMessageIds(QUERY))
                .containsExactly(new MessageId(FIRST_MESSAGE_ID), new MessageId(SECOND_MESSAGE_ID));
        assertThat(client.queries).containsExactly(QUERY, QUERY);
        assertThat(client.pageTokens).containsExactly(null, FIRST_PAGE_TOKEN);
    }

    @Test
    @DisplayName("treats an absent message collection as an empty page")
    void handlesAbsentMessages() {
        StubGmailApiClient client = new StubGmailApiClient();
        client.pages = List.of(new GmailMessagePage(null, null));

        assertThat(new GmailGateway(client).listMessageIds(QUERY)).isEmpty();
    }

    @Test
    @DisplayName("translates provider failures without exposing the access token")
    void translatesProviderFailure() {
        StubGmailApiClient client = new StubGmailApiClient();
        client.failure = new IOException(API_FAILURE);

        assertThatExceptionOfType(MailboxGatewayException.class)
                .isThrownBy(() -> new GmailGateway(client).listMessageIds(QUERY))
                .withMessageContaining(API_FAILURE)
                .withCause(client.failure);
    }

    @Test
    @DisplayName("fails loudly when Gmail repeats a page token")
    void rejectsRepeatedPageToken() {
        StubGmailApiClient client = new StubGmailApiClient();
        client.pages = List.of(
                new GmailMessagePage(List.of(FIRST_MESSAGE_ID), FIRST_PAGE_TOKEN),
                new GmailMessagePage(List.of(SECOND_MESSAGE_ID), FIRST_PAGE_TOKEN));

        assertThatExceptionOfType(MailboxGatewayException.class)
                .isThrownBy(() -> new GmailGateway(client).listMessageIds(QUERY))
                .withMessageContaining(REPEATED_PAGE_TOKEN_MESSAGE);
    }

    private static final class StubGmailApiClient implements GmailApiClient {
        private List<GmailLabel> labels = List.of();
        private List<GmailMessagePage> pages = List.of(new GmailMessagePage(List.of(), null));
        private final List<String> queries = new java.util.ArrayList<>();
        private final List<String> pageTokens = new java.util.ArrayList<>();
        private IOException failure;
        private int pageIndex;

        @Override
        public List<GmailLabel> listLabels() throws IOException {
            failWhenConfigured();
            return labels;
        }

        @Override
        public GmailMessagePage listMessageIds(String query, String pageToken) throws IOException {
            failWhenConfigured();
            queries.add(query);
            pageTokens.add(pageToken);
            return pages.get(pageIndex++);
        }

        @Override
        public GmailMetadataBatch getMessageMetadata(List<String> messageIds) {
            return new GmailMetadataBatch(List.of(), List.of());
        }

        @Override
        public void batchModifyLabels(
                List<String> messageIds, List<String> addLabelIds, List<String> removeLabelIds) {}

        private void failWhenConfigured() throws IOException {
            if (failure != null) {
                throw failure;
            }
        }
    }
}
