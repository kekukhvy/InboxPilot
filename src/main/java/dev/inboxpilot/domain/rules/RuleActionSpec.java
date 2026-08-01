package dev.inboxpilot.domain.rules;

import java.util.Map;
import java.util.Objects;

/** Persisted action name and its provider-independent parameters. */
public record RuleActionSpec(String type, Map<String, String> parameters) {

    public RuleActionSpec {
        Objects.requireNonNull(type, "type");
        if (type.isBlank()) {
            throw new IllegalArgumentException("Action type must not be blank");
        }
        parameters = Map.copyOf(parameters);
    }
}
