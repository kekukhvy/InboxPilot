package dev.inboxpilot.infrastructure.gmail;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import dev.inboxpilot.application.model.AccessToken;
import dev.inboxpilot.application.port.CredentialProvider;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Thin wrapper around Google's generated Gmail client. */
@Component
@SuppressWarnings("deprecation")
class GoogleGmailApiClient implements GmailApiClient {

    private static final String USER_ID = "me";
    private static final String APPLICATION_NAME = "InboxPilot";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String METADATA_FORMAT = "metadata";
    private static final String FROM_HEADER = "From";
    private static final String SUBJECT_HEADER = "Subject";
    private static final String RESPONSE_FIELDS = "id,internalDate,labelIds,payload(headers)";
    private static final String UNKNOWN_FAILURE = "Gmail returned no failure details";
    private static final String MISSING_HEADER_PREFIX = "Missing Gmail header: ";
    private static final String EMPTY_SUBJECT = "";
    private static final char ADDRESS_START = '<';
    private static final char ADDRESS_END = '>';
    private static final int TOO_MANY_REQUESTS = 429;
    private static final int SERVER_ERROR_START = 500;
    private static final int FORBIDDEN = 403;
    private static final String RATE_LIMIT_REASON = "rateLimitExceeded";
    private static final String USER_RATE_LIMIT_REASON = "userRateLimitExceeded";
    private static final String QUOTA_REASON = "quotaExceeded";
    private static final String CONCURRENT_REASON = "concurrentLimitExceeded";

    private final CredentialProvider credentialProvider;
    private Gmail gmail;
    private Instant expiresAt;

    GoogleGmailApiClient(CredentialProvider credentialProvider) {
        this.credentialProvider = credentialProvider;
    }

    @Override
    public List<GmailLabel> listLabels() throws IOException {
        ListLabelsResponse response = gmail().users().labels().list(USER_ID).execute();
        if (response.getLabels() == null) {
            return List.of();
        }
        return response.getLabels().stream().map(GoogleGmailApiClient::toLabel).toList();
    }

    @Override
    public GmailMessagePage listMessageIds(String query, String pageToken) throws IOException {
        Gmail.Users.Messages.List request = gmail().users().messages().list(USER_ID).setQ(query);
        if (pageToken != null) {
            request.setPageToken(pageToken);
        }
        ListMessagesResponse response = request.execute();
        return new GmailMessagePage(messageIds(response), response.getNextPageToken());
    }

    @Override
    public GmailMetadataBatch getMessageMetadata(List<String> messageIds) throws IOException {
        List<GmailMessageMetadata> messages = new ArrayList<>();
        List<GmailMetadataFailure> failures = new ArrayList<>();
        BatchRequest batch = gmail().batch();
        for (String messageId : messageIds) {
            queueMetadataRequest(batch, messageId, messages, failures);
        }
        batch.execute();
        return new GmailMetadataBatch(messages, failures);
    }

    private void queueMetadataRequest(
            BatchRequest batch,
            String messageId,
            List<GmailMessageMetadata> messages,
            List<GmailMetadataFailure> failures) throws IOException {
        gmail().users().messages().get(USER_ID, messageId)
                .setFormat(METADATA_FORMAT)
                .setMetadataHeaders(List.of(FROM_HEADER, SUBJECT_HEADER))
                .setFields(RESPONSE_FIELDS)
                .queue(batch, metadataCallback(messageId, messages, failures));
    }

    private static JsonBatchCallback<Message> metadataCallback(
            String messageId,
            List<GmailMessageMetadata> messages,
            List<GmailMetadataFailure> failures) {
        return new JsonBatchCallback<>() {
            @Override
            public void onSuccess(Message message, HttpHeaders responseHeaders) {
                try {
                    messages.add(toMetadata(message));
                } catch (RuntimeException exception) {
                    failures.add(new GmailMetadataFailure(
                            messageId, exception.getMessage(), false, false));
                }
            }

            @Override
            public void onFailure(GoogleJsonError error, HttpHeaders responseHeaders) {
                String reason = error == null ? UNKNOWN_FAILURE : error.getMessage();
                int statusCode = error == null ? 0 : error.getCode();
                boolean throttled = isThrottled(error, statusCode);
                boolean retryable = throttled || statusCode >= SERVER_ERROR_START;
                failures.add(new GmailMetadataFailure(
                        messageId, reason, retryable, throttled));
            }
        };
    }

    private static boolean isThrottled(GoogleJsonError error, int statusCode) {
        if (statusCode == TOO_MANY_REQUESTS) {
            return true;
        }
        if (statusCode != FORBIDDEN || error == null || error.getErrors() == null) {
            return false;
        }
        return error.getErrors().stream()
                .map(GoogleJsonError.ErrorInfo::getReason)
                .anyMatch(GoogleGmailApiClient::isThrottleReason);
    }

    private static boolean isThrottleReason(String reason) {
        return RATE_LIMIT_REASON.equals(reason)
                || USER_RATE_LIMIT_REASON.equals(reason)
                || QUOTA_REASON.equals(reason)
                || CONCURRENT_REASON.equals(reason);
    }

    private static GmailMessageMetadata toMetadata(Message message) {
        List<MessagePartHeader> headers = message.getPayload().getHeaders();
        String sender = extractAddress(headerValue(headers, FROM_HEADER));
        String subject = headerValueOrEmpty(headers, SUBJECT_HEADER);
        List<String> labels = message.getLabelIds() == null ? List.of() : message.getLabelIds();
        return new GmailMessageMetadata(
                message.getId(), sender, subject,
                Instant.ofEpochMilli(message.getInternalDate()), labels);
    }

    private static String headerValue(List<MessagePartHeader> headers, String name) {
        return headers.stream()
                .filter(header -> name.equalsIgnoreCase(header.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(MISSING_HEADER_PREFIX + name));
    }

    private static String headerValueOrEmpty(List<MessagePartHeader> headers, String name) {
        return headers.stream()
                .filter(header -> name.equalsIgnoreCase(header.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse(EMPTY_SUBJECT);
    }

    private static String extractAddress(String sender) {
        int start = sender.lastIndexOf(ADDRESS_START);
        int end = sender.lastIndexOf(ADDRESS_END);
        return start >= 0 && end > start ? sender.substring(start + 1, end) : sender;
    }

    private synchronized Gmail gmail() throws IOException {
        if (gmail == null || !Instant.now().isBefore(expiresAt)) {
            AccessToken token = credentialProvider.obtainAccessToken();
            gmail = buildClient(token);
            expiresAt = token.expiresAt();
        }
        return gmail;
    }

    private static Gmail buildClient(AccessToken token) throws IOException {
        try {
            return new Gmail.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    request -> request.getHeaders()
                            .set(AUTHORIZATION_HEADER, BEARER_PREFIX + token.value()))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (GeneralSecurityException exception) {
            throw new IOException(exception);
        }
    }

    private static GmailLabel toLabel(Label label) {
        return new GmailLabel(label.getId(), label.getName());
    }

    private static List<String> messageIds(ListMessagesResponse response) {
        List<Message> messages = response.getMessages();
        if (messages == null) {
            return List.of();
        }
        return messages.stream().map(Message::getId).toList();
    }
}
