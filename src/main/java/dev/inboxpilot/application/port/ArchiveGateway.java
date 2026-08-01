package dev.inboxpilot.application.port;

import dev.inboxpilot.domain.message.MessageId;
import java.util.List;

/** Removes only the inbox label from explicitly selected messages. */
public interface ArchiveGateway {

    void archive(List<MessageId> messageIds);
}
