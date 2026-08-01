package dev.inboxpilot.domain.analysis;

import java.util.Objects;

/** Provider-independent label identity used by mailbox analysis. */
public record LabelDefinition(String id, String name) {

    public LabelDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (id.isBlank() || name.isBlank()) {
            throw new IllegalArgumentException("Label id and name must not be blank");
        }
    }
}
