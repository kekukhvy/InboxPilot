package dev.inboxpilot.application.model;

import dev.inboxpilot.domain.message.MessageId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Explicit batch of user-label additions and removals. */
public record LabelBatchChange(
        List<MessageId> messageIds,
        List<String> addLabelIds,
        List<String> removeLabelIds) {

    private static final String USER_LABEL_PREFIX = "Label_";

    public LabelBatchChange {
        messageIds = List.copyOf(messageIds);
        addLabelIds = validatedLabels(addLabelIds);
        removeLabelIds = validatedLabels(removeLabelIds);
        if (messageIds.isEmpty()) {
            throw new IllegalArgumentException("A label batch must contain at least one message");
        }
        if (addLabelIds.isEmpty() && removeLabelIds.isEmpty()) {
            throw new IllegalArgumentException("A label batch must contain at least one change");
        }
        Set<String> overlap = new HashSet<>(addLabelIds);
        overlap.retainAll(removeLabelIds);
        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException("A label cannot be added and removed in the same batch");
        }
    }

    private static List<String> validatedLabels(List<String> labelIds) {
        List<String> labels = List.copyOf(labelIds);
        if (labels.stream().anyMatch(label -> !label.startsWith(USER_LABEL_PREFIX))) {
            throw new IllegalArgumentException("Only Gmail user-label IDs may be changed");
        }
        return labels;
    }
}
