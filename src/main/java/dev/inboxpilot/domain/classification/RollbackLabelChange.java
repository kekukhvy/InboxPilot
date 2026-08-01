package dev.inboxpilot.domain.classification;

import dev.inboxpilot.domain.message.MessageId;
import java.util.List;

/** Inverse user-label operations for one classified message. */
public record RollbackLabelChange(
        MessageId messageId,
        List<String> addLabelIds,
        List<String> removeLabelIds) {

    public RollbackLabelChange {
        addLabelIds = List.copyOf(addLabelIds);
        removeLabelIds = List.copyOf(removeLabelIds);
    }
}
