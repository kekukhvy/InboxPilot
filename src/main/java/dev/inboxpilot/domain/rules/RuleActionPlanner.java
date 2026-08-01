package dev.inboxpilot.domain.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Converts matched rule actions into an inert, provider-independent label plan. */
public final class RuleActionPlanner {

    private static final String LABEL_PARAMETER = "label";

    public LabelChangePlan plan(RuleEvaluation evaluation) {
        List<PlannedLabelChange> changes = new ArrayList<>();
        for (RuleMatch match : evaluation.matches()) {
            for (RuleActionSpec action : match.actions()) {
                changes.add(compile(match.id(), action));
            }
        }
        return new LabelChangePlan(changes);
    }

    private static PlannedLabelChange compile(RuleId ruleId, RuleActionSpec action) {
        LabelChangeType type = switch (action.type()) {
            case "add-label" -> LabelChangeType.ADD;
            case "remove-label" -> LabelChangeType.REMOVE;
            default -> throw new IllegalArgumentException("Unsupported rule action: " + action.type());
        };
        return new PlannedLabelChange(ruleId, type, requiredLabel(action.parameters()));
    }

    private static String requiredLabel(Map<String, String> parameters) {
        String label = parameters.get(LABEL_PARAMETER);
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Rule label action requires parameter 'label'");
        }
        return label;
    }
}
