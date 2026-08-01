package dev.inboxpilot.domain.classification;

import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.domain.rules.RuleId;
import java.util.List;

/** Exact old, changed, and desired label state for one message. */
public record MessageLabelPlan(
        MessageId messageId,
        List<RuleId> matchedRuleIds,
        List<String> oldLabels,
        List<String> addLabels,
        List<String> removeLabels,
        List<String> newLabels) {

    public MessageLabelPlan {
        matchedRuleIds = List.copyOf(matchedRuleIds);
        oldLabels = List.copyOf(oldLabels);
        addLabels = List.copyOf(addLabels);
        removeLabels = List.copyOf(removeLabels);
        newLabels = List.copyOf(newLabels);
    }

    public boolean changesLabels() {
        return !addLabels.isEmpty() || !removeLabels.isEmpty();
    }
}
