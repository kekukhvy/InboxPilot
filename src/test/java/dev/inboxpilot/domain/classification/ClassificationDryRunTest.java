package dev.inboxpilot.domain.classification;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.domain.rules.RuleActionSpec;
import dev.inboxpilot.domain.rules.RuleConditionSpec;
import dev.inboxpilot.domain.rules.RuleConflictBehavior;
import dev.inboxpilot.domain.rules.RuleDefinition;
import dev.inboxpilot.domain.rules.RuleId;
import dev.inboxpilot.domain.rules.RuleMetadata;
import dev.inboxpilot.domain.rules.RuleSet;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClassificationDryRun")
class ClassificationDryRunTest {

    @Test
    @DisplayName("produces exact additions, removals, and final labels without mailbox access")
    void producesExactLabelDiff() {
        MailMessage message = new MailMessage(
                new MessageId("m1"), new EmailAddress("news@example.com"), "Digest",
                Instant.parse("2026-01-01T00:00:00Z"), List.of("INBOX", "Old"));
        RuleSet rules = new RuleSet(1, List.of(rule()));

        ClassificationPlan plan = new ClassificationDryRun().plan(rules, List.of(message));

        assertThat(plan.messages()).singleElement().satisfies(change -> {
            assertThat(change.messageId()).isEqualTo(new MessageId("m1"));
            assertThat(change.matchedRuleIds()).containsExactly(new RuleId("newsletter"));
            assertThat(change.oldLabels()).containsExactly("INBOX", "Old");
            assertThat(change.addLabels()).containsExactly("Newsletters");
            assertThat(change.removeLabels()).containsExactly("Old");
            assertThat(change.newLabels()).containsExactly("INBOX", "Newsletters");
        });
    }

    private static RuleDefinition rule() {
        return new RuleDefinition(
                new RuleId("newsletter"), 100,
                new RuleConditionSpec("sender-domain", Map.of("value", "example.com"), List.of()),
                List.of(
                        new RuleActionSpec("add-label", Map.of("label", "Newsletters")),
                        new RuleActionSpec("remove-label", Map.of("label", "Old"))),
                RuleConflictBehavior.CONTINUE,
                new RuleMetadata("Newsletter", "test", List.of()));
    }
}
