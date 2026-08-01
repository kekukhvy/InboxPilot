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

@DisplayName("RepeatRunVerifier")
class RepeatRunVerifierTest {

    @Test
    @DisplayName("reports zero additional changes after the desired state was applied")
    void verifiesIdempotency() {
        MailMessage applied = message("applied", List.of("INBOX", "Label_Newsletters"));

        RepeatRunVerification verification = new RepeatRunVerifier()
                .verify(rules(), List.of(applied));

        assertThat(verification.idempotent()).isTrue();
        assertThat(verification.additionalChangeMessageIds()).isEmpty();
        assertThat(verification.evaluatedMessages()).isEqualTo(1);
    }

    @Test
    @DisplayName("identifies messages that would still change")
    void identifiesRemainingChanges() {
        MessageId messageId = new MessageId("pending");

        RepeatRunVerification verification = new RepeatRunVerifier()
                .verify(rules(), List.of(message(messageId.value(), List.of("INBOX", "Label_Old"))));

        assertThat(verification.idempotent()).isFalse();
        assertThat(verification.additionalChangeMessageIds()).containsExactly(messageId);
    }

    private static MailMessage message(String id, List<String> labels) {
        return new MailMessage(
                new MessageId(id), new EmailAddress("news@example.com"), "Digest",
                Instant.parse("2026-01-01T00:00:00Z"), labels);
    }

    private static RuleSet rules() {
        RuleDefinition rule = new RuleDefinition(
                new RuleId("newsletter"), 100,
                new RuleConditionSpec("sender-domain", Map.of("value", "example.com"), List.of()),
                List.of(
                        new RuleActionSpec("add-label", Map.of("label", "Label_Newsletters")),
                        new RuleActionSpec("remove-label", Map.of("label", "Label_Old"))),
                RuleConflictBehavior.CONTINUE,
                new RuleMetadata("Newsletter", "test", List.of()));
        return new RuleSet(1, List.of(rule));
    }
}
