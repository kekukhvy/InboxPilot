package dev.inboxpilot.domain.classification;

import java.util.List;

/** Versioned inverse operation artifact generated before classification execution. */
public record RollbackPlan(int version, List<RollbackLabelChange> changes) {

    public static final int CURRENT_VERSION = 1;

    public RollbackPlan {
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException("Unsupported rollback plan version: " + version);
        }
        changes = List.copyOf(changes);
    }
}
