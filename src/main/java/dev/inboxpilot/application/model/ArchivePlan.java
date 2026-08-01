package dev.inboxpilot.application.model;

import dev.inboxpilot.domain.message.MessageId;
import java.util.List;

/** Selected messages previewed for archive, never deletion. */
public record ArchivePlan(List<MessageId> messageIds) {

    public ArchivePlan {
        messageIds = messageIds.stream().distinct().toList();
    }
}
