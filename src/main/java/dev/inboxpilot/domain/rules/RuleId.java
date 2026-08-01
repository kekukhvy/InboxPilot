package dev.inboxpilot.domain.rules;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable rule identifier suitable for YAML references and audit logs. */
public record RuleId(String value) {

    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9][a-z0-9_-]*");

    public RuleId {
        Objects.requireNonNull(value, "value");
        if (!VALID_ID.matcher(value).matches()) {
            throw new IllegalArgumentException("Rule id must use lowercase letters, digits, hyphens, or underscores");
        }
    }
}
