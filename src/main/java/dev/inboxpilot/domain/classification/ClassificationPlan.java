package dev.inboxpilot.domain.classification;

import java.util.List;

/** Immutable dry-run result for an ordered message batch. */
public record ClassificationPlan(List<MessageLabelPlan> messages) {

    public ClassificationPlan {
        messages = List.copyOf(messages);
    }
}
