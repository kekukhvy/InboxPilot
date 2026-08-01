package dev.inboxpilot.infrastructure.gmail;

import dev.inboxpilot.application.model.MailboxLabel;
import dev.inboxpilot.application.port.MailboxGateway;
import dev.inboxpilot.application.port.MailboxGatewayException;
import dev.inboxpilot.domain.message.MessageId;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Gmail implementation of the mailbox catalog gateway. */
@Component
public class GmailGateway implements MailboxGateway {

    private static final String LABEL_FAILURE = "Gmail labels could not be listed: ";
    private static final String MESSAGE_FAILURE = "Gmail message ids could not be listed: ";
    private static final String REPEATED_PAGE_TOKEN =
            "Gmail returned a repeated page token while listing message ids";
    private static final String QUERY_PARAMETER = "query";
    private static final Logger logger = LoggerFactory.getLogger(GmailGateway.class);

    private final GmailApiClient client;

    public GmailGateway(GmailApiClient client) {
        this.client = client;
    }

    @Override
    public List<MailboxLabel> listLabels() {
        try {
            return client.listLabels().stream()
                    .map(label -> new MailboxLabel(label.id(), label.name()))
                    .toList();
        } catch (IOException exception) {
            throw translate(LABEL_FAILURE, exception);
        }
    }

    @Override
    public List<MessageId> listMessageIds(String query) {
        Objects.requireNonNull(query, QUERY_PARAMETER);
        try {
            return collectMessageIds(query);
        } catch (IOException exception) {
            throw translate(MESSAGE_FAILURE, exception);
        }
    }

    private List<MessageId> collectMessageIds(String query) throws IOException {
        List<MessageId> messageIds = new ArrayList<>();
        Set<String> visitedPageTokens = new HashSet<>();
        String pageToken = null;
        do {
            GmailMessagePage page = client.listMessageIds(query, pageToken);
            addMessageIds(messageIds, page.messageIds());
            pageToken = nextPageToken(page.nextPageToken(), visitedPageTokens);
        } while (pageToken != null);
        return List.copyOf(messageIds);
    }

    private static void addMessageIds(List<MessageId> destination, List<String> source) {
        if (source != null) {
            source.stream().map(MessageId::new).forEach(destination::add);
        }
    }

    private static String nextPageToken(String token, Set<String> visitedTokens) {
        if (token != null && !visitedTokens.add(token)) {
            throw new MailboxGatewayException(REPEATED_PAGE_TOKEN);
        }
        return token;
    }

    private MailboxGatewayException translate(String prefix, IOException exception) {
        logger.warn("{}{}", prefix, exception.getMessage());
        return new MailboxGatewayException(prefix + exception.getMessage(), exception);
    }
}
