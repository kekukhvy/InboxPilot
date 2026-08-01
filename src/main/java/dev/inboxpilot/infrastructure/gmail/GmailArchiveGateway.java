package dev.inboxpilot.infrastructure.gmail;

import dev.inboxpilot.application.port.ArchiveGateway;
import dev.inboxpilot.application.port.MailboxGatewayException;
import dev.inboxpilot.domain.message.MessageId;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;

/** Gmail archive adapter implemented solely as removal of the INBOX label. */
@Component
public final class GmailArchiveGateway implements ArchiveGateway {

    private static final String INBOX_LABEL_ID = "INBOX";
    private static final String FAILURE_MESSAGE = "Gmail batch archive failed";
    private final GmailApiClient client;

    GmailArchiveGateway(GmailApiClient client) {
        this.client = client;
    }

    @Override
    public void archive(List<MessageId> messageIds) {
        try {
            client.batchModifyLabels(
                    messageIds.stream().map(MessageId::value).toList(),
                    List.of(), List.of(INBOX_LABEL_ID));
        } catch (IOException exception) {
            throw new MailboxGatewayException(FAILURE_MESSAGE, exception);
        }
    }
}
