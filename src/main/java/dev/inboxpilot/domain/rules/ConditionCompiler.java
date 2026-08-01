package dev.inboxpilot.domain.rules;

import java.util.List;

/** Recursively compiles leaf and composite condition specifications. */
public final class ConditionCompiler {

    private static final String ALL = "all";
    private static final String ANY = "any";
    private static final String NOT = "not";
    private final LeafConditionCompiler leafCompiler;

    public ConditionCompiler() {
        this(new LeafConditionCompiler());
    }

    ConditionCompiler(LeafConditionCompiler leafCompiler) {
        this.leafCompiler = leafCompiler;
    }

    public MessageCondition compile(RuleConditionSpec specification) {
        return switch (specification.operator()) {
            case ALL -> all(specification);
            case ANY -> any(specification);
            case NOT -> not(specification);
            default -> leafCompiler.compile(specification);
        };
    }

    private MessageCondition all(RuleConditionSpec specification) {
        List<MessageCondition> operands = operands(specification, ALL);
        return message -> operands.stream().allMatch(condition -> condition.matches(message));
    }

    private MessageCondition any(RuleConditionSpec specification) {
        List<MessageCondition> operands = operands(specification, ANY);
        return message -> operands.stream().anyMatch(condition -> condition.matches(message));
    }

    private MessageCondition not(RuleConditionSpec specification) {
        if (specification.operands().size() != 1) {
            throw new IllegalArgumentException("Composite 'not' requires exactly one operand");
        }
        MessageCondition operand = compile(specification.operands().get(0));
        return message -> !operand.matches(message);
    }

    private List<MessageCondition> operands(RuleConditionSpec specification, String operator) {
        if (specification.operands().isEmpty()) {
            throw new IllegalArgumentException("Composite '" + operator + "' requires at least one operand");
        }
        return specification.operands().stream().map(this::compile).toList();
    }
}
