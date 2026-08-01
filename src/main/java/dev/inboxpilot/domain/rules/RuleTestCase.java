package dev.inboxpilot.domain.rules;

import dev.inboxpilot.domain.message.EmailAddress;
import java.util.Objects;

/** Metadata example embedded in YAML with its expected match outcome. */
public record RuleTestCase(
        String name,
        EmailAddress sender,
        String subject,
        boolean expectedMatch) {

    public RuleTestCase {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(subject, "subject");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Rule test name must not be blank");
        }
    }
}
