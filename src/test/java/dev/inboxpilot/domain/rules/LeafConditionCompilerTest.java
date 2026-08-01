package dev.inboxpilot.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("LeafConditionCompiler")
class LeafConditionCompilerTest {

    private static final MailMessage MESSAGE = new MailMessage(
            new MessageId("message"),
            new EmailAddress("News@Updates.Example.com"),
            "Weekly Digest 42",
            Instant.parse("2026-01-01T00:00:00Z"),
            List.of());

    @ParameterizedTest(name = "{0}")
    @MethodSource("matchingConditions")
    @DisplayName("supports sender, domain, suffix, subject, regex, and ignore-case matching")
    void supportsLeafMatchers(String name, RuleConditionSpec specification) {
        MessageCondition condition = new LeafConditionCompiler().compile(specification);

        assertThat(condition.matches(MESSAGE)).as(name).isTrue();
    }

    @Test
    @DisplayName("does not confuse a textual ending with a domain suffix boundary")
    void respectsDomainBoundary() {
        RuleConditionSpec specification = new RuleConditionSpec(
                "sender-domain-suffix", Map.of("value", "ample.com"), List.of());

        assertThat(new LeafConditionCompiler().compile(specification).matches(MESSAGE)).isFalse();
    }

    @Test
    @DisplayName("keeps subject matching case-sensitive unless explicitly disabled")
    void keepsDefaultCaseSensitivity() {
        RuleConditionSpec specification = new RuleConditionSpec(
                "subject-contains", Map.of("value", "weekly"), List.of());

        assertThat(new LeafConditionCompiler().compile(specification).matches(MESSAGE)).isFalse();
    }

    static Stream<Arguments> matchingConditions() {
        return Stream.of(
                condition("exact sender", "sender-exact", "news@updates.example.com", true),
                condition("exact domain", "sender-domain", "updates.example.com", false),
                condition("domain suffix", "sender-domain-suffix", "example.com", false),
                condition("subject contains", "subject-contains", "weekly digest", true),
                condition("subject regex", "subject-regex", "digest \\d+", true));
    }

    private static Arguments condition(
            String name, String operator, String value, boolean ignoreCase) {
        return Arguments.of(name, new RuleConditionSpec(
                operator,
                Map.of("value", value, "ignore-case", Boolean.toString(ignoreCase)),
                List.of()));
    }
}
