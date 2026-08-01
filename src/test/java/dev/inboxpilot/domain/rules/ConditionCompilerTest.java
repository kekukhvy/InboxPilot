package dev.inboxpilot.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConditionCompiler")
class ConditionCompilerTest {

    private static final MailMessage MESSAGE = new MailMessage(
            new MessageId("message"), new EmailAddress("news@example.com"),
            "Weekly digest", Instant.parse("2026-01-01T00:00:00Z"), List.of());

    @Test
    @DisplayName("evaluates nested all, any, and not conditions")
    void evaluatesNestedConditions() {
        RuleConditionSpec condition = composite("all",
                leaf("sender-domain", "example.com"),
                composite("any",
                        leaf("subject-contains", "missing"),
                        composite("not", leaf("subject-contains", "promotion"))));

        assertThat(new ConditionCompiler().compile(condition).matches(MESSAGE)).isTrue();
    }

    @Test
    @DisplayName("rejects empty composites and not with more than one operand")
    void rejectsInvalidOperandCounts() {
        ConditionCompiler compiler = new ConditionCompiler();

        assertThatThrownBy(() -> compiler.compile(composite("all")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one");
        assertThatThrownBy(() -> compiler.compile(composite(
                        "not", leaf("subject-contains", "one"), leaf("subject-contains", "two"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one");
    }

    private static RuleConditionSpec composite(String operator, RuleConditionSpec... operands) {
        return new RuleConditionSpec(operator, Map.of(), List.of(operands));
    }

    private static RuleConditionSpec leaf(String operator, String value) {
        return new RuleConditionSpec(operator, Map.of("value", value), List.of());
    }
}
