package dev.inboxpilot.domain.rules;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Persisted condition node; leaf parameters and composite operands share one recursive shape. */
public record RuleConditionSpec(
        String operator,
        Map<String, String> parameters,
        List<RuleConditionSpec> operands) {

    public RuleConditionSpec {
        Objects.requireNonNull(operator, "operator");
        if (operator.isBlank()) {
            throw new IllegalArgumentException("Condition operator must not be blank");
        }
        parameters = Map.copyOf(parameters);
        operands = List.copyOf(operands);
    }
}
