package dev.inboxpilot.application.port;

import dev.inboxpilot.domain.message.MailMessage;
import java.time.Instant;
import java.util.List;

/**
 * Port for reading messages from a mailbox.
 *
 * <p>Declared by the application layer and expressed purely in domain terms, so
 * use cases never learn which provider is behind it. Implemented by adapters in
 * {@code dev.inboxpilot.infrastructure} — see ADR 0001.
 */
public interface MessageSource {

    /**
     * Fetches messages that arrived at or after the given instant.
     *
     * @param since the earliest arrival time to include
     * @return the matching messages, oldest first; empty when there are none
     * @throws MessageSourceException if the mailbox cannot be read
     */
    List<MailMessage> fetchSince(Instant since);
}
