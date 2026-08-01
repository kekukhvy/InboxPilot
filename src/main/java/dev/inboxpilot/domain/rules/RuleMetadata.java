package dev.inboxpilot.domain.rules;

import java.util.List;
import java.util.Objects;

/** Human-facing provenance and description retained with a rule. */
public record RuleMetadata(String description, String source, List<String> tags) {

    public RuleMetadata {
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(source, "source");
        tags = List.copyOf(tags);
    }
}
