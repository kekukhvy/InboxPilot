package dev.inboxpilot.application.port;

import dev.inboxpilot.application.model.MailboxLabel;
import dev.inboxpilot.domain.message.MessageId;
import java.util.List;

/** Reads mailbox catalog data without exposing provider client types. */
public interface MailboxGateway {

    /** @return every label visible to the authorized mailbox user */
    List<MailboxLabel> listLabels();

    /**
     * Lists every message identifier matching a provider search expression.
     *
     * @param query search expression accepted by the configured mailbox provider
     * @return all matching identifiers across every provider page
     */
    List<MessageId> listMessageIds(String query);
}
