package dev.inboxpilot.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RuleEvaluator")
class RuleEvaluatorTest {

    private static final MailMessage MESSAGE = new MailMessage(
            new MessageId("message"), new EmailAddress("sender@example.com"),
            "Digest", Instant.parse("2026-01-01T00:00:00Z"), List.of());

    @Test
    @DisplayName("orders by descending priority then ID and stops after a matching stop rule")
    void appliesPriorityAndStopSemantics() {
        RuleSet rules = new RuleSet(1, List.of(
                rule("later", 10, RuleConflictBehavior.CONTINUE),
                rule("b-stop", 20, RuleConflictBehavior.STOP),
                rule("a-first", 20, RuleConflictBehavior.CONTINUE),
                rule("never", 5, RuleConflictBehavior.CONTINUE)));

        RuleEvaluation evaluation = new RuleEvaluator().evaluate(rules, MESSAGE);

        assertThat(evaluation.matches()).extracting(match -> match.id().value())
                .containsExactly("a-first", "b-stop");
        assertThat(evaluation.stopped()).isTrue();
    }

    @Test
    void ordersExactSenderBeforeRootDomainAtEqualPriority() {
        int priority = 100;
        RuleDefinition domain = rule("domain", priority, RuleConflictBehavior.CONTINUE);
        RuleDefinition sender = new RuleDefinition(
                new RuleId("sender"), priority,
                new RuleConditionSpec(
                        "sender-exact", Map.of("value", "sender@example.com"), List.of()),
                List.of(new RuleActionSpec("add-label", Map.of("label", "Specific"))),
                RuleConflictBehavior.CONTINUE,
                new RuleMetadata("Specific sender", "test", List.of()));

        RuleEvaluation evaluation = new RuleEvaluator().evaluate(
                new RuleSet(RuleSet.CURRENT_VERSION, List.of(domain, sender)), MESSAGE);

        assertThat(evaluation.matches()).extracting(match -> match.id().value())
                .containsExactly("sender", "domain");
    }

    @Test
    void ordersSenderWithSubjectBeforePlainExactSender() {
        int priority = 100;
        RuleDefinition exact = new RuleDefinition(
                new RuleId("exact"), priority,
                new RuleConditionSpec(
                        "sender-exact", Map.of("value", "sender@example.com"), List.of()),
                List.of(new RuleActionSpec("add-label", Map.of("label", "Exact"))),
                RuleConflictBehavior.CONTINUE,
                new RuleMetadata("Exact sender", "test", List.of()));
        RuleDefinition composite = new RuleDefinition(
                new RuleId("composite"), priority,
                new RuleConditionSpec("all", Map.of(), List.of(
                        exact.condition(),
                        new RuleConditionSpec(
                                "subject-contains", Map.of("value", "Digest"), List.of()))),
                List.of(new RuleActionSpec("add-label", Map.of("label", "Specific"))),
                RuleConflictBehavior.CONTINUE,
                new RuleMetadata("Sender and subject", "test", List.of()));

        RuleEvaluation evaluation = new RuleEvaluator().evaluate(
                new RuleSet(RuleSet.CURRENT_VERSION, List.of(exact, composite)), MESSAGE);

        assertThat(evaluation.matches()).extracting(match -> match.id().value())
                .containsExactly("composite", "exact");
    }

    private static RuleDefinition rule(String id, int priority, RuleConflictBehavior behavior) {
        return new RuleDefinition(
                new RuleId(id), priority,
                new RuleConditionSpec("sender-domain", Map.of("value", "example.com"), List.of()),
                List.of(new RuleActionSpec("add-label", Map.of("label", id))),
                behavior,
                new RuleMetadata("Test rule", "test", List.of()));
    }
}
