package dev.inboxpilot.domain.rules;

import java.util.List;

/** Ordered, inert label changes awaiting an explicit execution step. */
public record LabelChangePlan(List<PlannedLabelChange> changes) {

    public LabelChangePlan {
        changes = List.copyOf(changes);
    }
}
