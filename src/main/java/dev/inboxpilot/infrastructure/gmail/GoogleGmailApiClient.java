package dev.inboxpilot.infrastructure.gmail;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import dev.inboxpilot.application.model.AccessToken;
import dev.inboxpilot.application.port.CredentialProvider;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
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
