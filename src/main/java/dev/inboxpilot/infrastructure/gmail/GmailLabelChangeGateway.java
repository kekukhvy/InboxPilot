package dev.inboxpilot.infrastructure.gmail;

import dev.inboxpilot.application.model.LabelBatchChange;
import dev.inboxpilot.application.port.LabelChangeGateway;
import dev.inboxpilot.application.port.MailboxGatewayException;
import java.io.IOException;
import org.springframework.stereotype.Component;

/** Gmail adapter restricted to batch user-label additions and removals. */
@Component
public final class GmailLabelChangeGateway implements LabelChangeGateway {

    private static final String FAILURE_MESSAGE = "Gmail batch label change failed";
    private final GmailApiClient client;

    GmailLabelChangeGateway(GmailApiClient client) {
        this.client = client;
    }

    @Override
    public void apply(LabelBatchChange change) {
        try {
            client.batchModifyLabels(
                    change.messageIds().stream().map(messageId -> messageId.value()).toList(),
                    change.addLabelIds(),
                    change.removeLabelIds());
        } catch (IOException exception) {
            throw new MailboxGatewayException(FAILURE_MESSAGE, exception);
        }
    }
}
