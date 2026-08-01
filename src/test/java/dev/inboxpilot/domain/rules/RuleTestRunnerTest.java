package dev.inboxpilot.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.message.EmailAddress;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RuleTestRunner")
class RuleTestRunnerTest {

    @Test
    @DisplayName("reports matching and non-matching YAML examples")
    void runsExamples() {
        RuleDefinition rule = new RuleDefinition(
                new RuleId("example"), 1,
                new RuleConditionSpec("sender-domain", Map.of("value", "example.com"), List.of()),
                List.of(new RuleActionSpec("add-label", Map.of("label", "Example"))),
                RuleConflictBehavior.CONTINUE,
                new RuleMetadata("Example", "test", List.of()),
                List.of(
                        test("matching", "sender@example.com", true),
                        test("non-matching", "sender@other.com", false)));

        RuleTestReport report = new RuleTestRunner().run(rule);

        assertThat(report.passed()).isTrue();
        assertThat(report.results()).extracting(RuleTestResult::passed).containsExactly(true, true);
    }

    private static RuleTestCase test(String name, String sender, boolean expected) {
        return new RuleTestCase(name, new EmailAddress(sender), "Subject", expected);
    }
}
