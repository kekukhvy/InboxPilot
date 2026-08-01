package dev.inboxpilot.domain.classification;

import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.domain.rules.RuleSet;
import java.util.Collection;
import java.util.List;

/** Re-evaluates post-execution metadata and proves whether another run is a no-op. */
public final class RepeatRunVerifier {

    private final ClassificationDryRun dryRun = new ClassificationDryRun();

    public RepeatRunVerification verify(
            RuleSet rules, Collection<MailMessage> postExecutionMessages) {
        ClassificationPlan plan = dryRun.plan(rules, postExecutionMessages);
        List<MessageId> additionalChanges = plan.messages().stream()
                .filter(MessageLabelPlan::changesLabels)
                .map(MessageLabelPlan::messageId)
                .toList();
        return new RepeatRunVerification(plan.messages().size(), additionalChanges);
    }
}
