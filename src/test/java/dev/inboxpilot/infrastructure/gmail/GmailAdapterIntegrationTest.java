package dev.inboxpilot.infrastructure.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.services.gmail.Gmail;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.infrastructure.config.BatchingProperties;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Gmail adapter HTTP integration")
class GmailAdapterIntegrationTest {

    private static final String ROOT_URL = "https://gmail.test/";
    private static final String APPLICATION_NAME = "InboxPilot integration test";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String MULTIPART_CONTENT_TYPE = "multipart/mixed; boundary=batch_test";
    private static final String FIRST_PAGE =
            "{\"messages\":[{\"id\":\"message-1\"}],\"nextPageToken\":\"next\"}";
    private static final String SECOND_PAGE = "{\"messages\":[{\"id\":\"message-2\"}]}";
    private static final String SUCCESS_BATCH = batchResponse(successPart("message-1"));
    private static final String PARTIAL_BATCH = batchResponse(
            successPart("message-1"), throttledPart());
    private static final String RETRY_BATCH = batchResponse(successPart("message-2"));
    private static final String FIRST_ID = "message-1";
    private static final String SECOND_ID = "message-2";
    private static final String SENDER = "sender@example.com";
    private static final String SUBJECT = "Subject";
    private static final String LABEL = "INBOX";
    private static final String BATCH_BOUNDARY = "--batch_test";
    private static final Instant SINCE = Instant.parse("2026-08-01T00:00:00Z");
    private static final Duration NO_BACKOFF = Duration.ZERO;
    private static final int BATCH_SIZE = 2;
    private static final int ONE_RETRY = 1;
    private static final int OK = 200;

    @Test
    @DisplayName("follows Gmail pagination using mocked HTTP responses")
    void followsPagination() {
        QueueTransport transport = new QueueTransport(
                jsonResponse(FIRST_PAGE), jsonResponse(SECOND_PAGE));
        GmailGateway gateway = new GmailGateway(client(transport));

        assertThat(gateway.listMessageIds("after:1"))
                .extracting(messageId -> messageId.value())
                .containsExactly(FIRST_ID, SECOND_ID);
        assertThat(transport.urls).hasSize(2);
    }

    @Test
    @DisplayName("keeps successes and retries a throttled batch item over HTTP")
    void retriesPartialBatchFailure() {
        QueueTransport transport = new QueueTransport(
                jsonResponse("{\"messages\":[{\"id\":\"message-1\"},{\"id\":\"message-2\"}]}"),
                multipartResponse(PARTIAL_BATCH), multipartResponse(RETRY_BATCH));
        BatchingProperties properties = new BatchingProperties(
                BATCH_SIZE, ONE_RETRY, NO_BACKOFF, NO_BACKOFF);
        GmailMessageSource source = new GmailMessageSource(
                client(transport), properties, duration -> { });

        List<MailMessage> messages = source.fetchSince(SINCE);

        assertThat(messages).extracting(message -> message.id().value())
                .containsExactly(FIRST_ID, SECOND_ID);
        assertThat(transport.requestBodies.getLast()).contains(SECOND_ID).doesNotContain(FIRST_ID);
    }

    @Test
    @DisplayName("sends only explicit label IDs through Gmail batchModify")
    void batchModifiesLabels() throws IOException {
        QueueTransport transport = new QueueTransport(jsonResponse("{}"));

        client(transport).batchModifyLabels(
                List.of(FIRST_ID, SECOND_ID), List.of("Label_Add"), List.of("Label_Remove"));

        assertThat(transport.urls).singleElement().asString().contains("messages/batchModify");
        assertThat(transport.requestBodies).singleElement().asString()
                .contains(FIRST_ID, SECOND_ID, "Label_Add", "Label_Remove")
                .doesNotContain("UNREAD", "STARRED", "IMPORTANT", "INBOX", "TRASH");
    }

    private static GoogleGmailApiClient client(QueueTransport transport) {
        Gmail gmail = new Gmail.Builder(transport, GsonFactory.getDefaultInstance(), request -> { })
                .setRootUrl(ROOT_URL)
                .setApplicationName(APPLICATION_NAME)
                .build();
        return new GoogleGmailApiClient(gmail);
    }

    private static MockLowLevelHttpResponse jsonResponse(String content) {
        return new MockLowLevelHttpResponse()
                .setStatusCode(OK).setContentType(JSON_CONTENT_TYPE).setContent(content);
    }

    private static MockLowLevelHttpResponse multipartResponse(String content) {
        return new MockLowLevelHttpResponse()
                .setStatusCode(OK).setContentType(MULTIPART_CONTENT_TYPE).setContent(content);
    }

    private static String batchResponse(String... parts) {
        return String.join("\r\n", parts) + "\r\n" + BATCH_BOUNDARY + "--\r\n";
    }

    private static String successPart(String messageId) {
        String json = "{\"id\":\"" + messageId + "\",\"internalDate\":\"1785542400000\","
                + "\"labelIds\":[\"" + LABEL + "\"],\"payload\":{\"headers\":["
                + "{\"name\":\"From\",\"value\":\"" + SENDER + "\"},"
                + "{\"name\":\"Subject\",\"value\":\"" + SUBJECT + "\"}]}}";
        return BATCH_BOUNDARY + "\r\nContent-Type: application/http\r\n\r\n"
                + "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n" + json;
    }

    private static String throttledPart() {
        String json = "{\"error\":{\"code\":429,\"message\":\"rate limited\"}}";
        return BATCH_BOUNDARY + "\r\nContent-Type: application/http\r\n\r\n"
                + "HTTP/1.1 429 Too Many Requests\r\nContent-Type: application/json\r\n\r\n"
                + json;
    }

    private static final class QueueTransport extends MockHttpTransport {
        private final Queue<MockLowLevelHttpResponse> responses;
        private final List<String> urls = new ArrayList<>();
        private final List<String> requestBodies = new ArrayList<>();

        private QueueTransport(MockLowLevelHttpResponse... responses) {
            this.responses = new ArrayDeque<>(List.of(responses));
        }

        @Override
        public LowLevelHttpRequest buildRequest(String method, String url) {
            urls.add(url);
            MockLowLevelHttpResponse response = responses.remove();
            return new MockLowLevelHttpRequest(url) {
                @Override
                public com.google.api.client.http.LowLevelHttpResponse execute() throws IOException {
                    requestBodies.add(getContentAsString());
                    return response;
                }
            };
        }
    }
}
