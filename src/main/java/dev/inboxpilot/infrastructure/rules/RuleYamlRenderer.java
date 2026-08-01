package dev.inboxpilot.infrastructure.rules;

import dev.inboxpilot.domain.rules.RuleActionSpec;
import dev.inboxpilot.domain.rules.RuleConditionSpec;
import dev.inboxpilot.domain.rules.RuleDefinition;
import dev.inboxpilot.domain.rules.RuleSet;
import dev.inboxpilot.domain.rules.RuleTestCase;
import java.util.Map;

/** Renders domain rules into deterministic schema-compatible YAML. */
public final class RuleYamlRenderer {

    public String render(RuleSet ruleSet) {
        StringBuilder yaml = new StringBuilder("version: ")
                .append(ruleSet.version()).append("\nrules:\n");
        ruleSet.rules().forEach(rule -> appendRule(yaml, rule));
        return yaml.toString();
    }

    private static void appendRule(StringBuilder yaml, RuleDefinition rule) {
        yaml.append("  - id: ").append(quoted(rule.id().value())).append('\n')
                .append("    priority: ").append(rule.priority()).append('\n')
                .append("    match:\n");
        appendCondition(yaml, rule.condition(), 6);
        yaml.append("    actions:\n");
        rule.actions().forEach(action -> appendAction(yaml, action));
        yaml.append("    conflict-behavior: ")
                .append(rule.conflictBehavior().name().toLowerCase()).append('\n')
                .append("    metadata:\n")
                .append("      description: ").append(quoted(rule.metadata().description())).append('\n')
                .append("      source: ").append(quoted(rule.metadata().source())).append('\n')
                .append("      tags:");
        rule.metadata().tags().forEach(tag -> yaml.append("\n        - ").append(quoted(tag)));
        yaml.append('\n');
        if (!rule.tests().isEmpty()) {
            yaml.append("    tests:\n");
            rule.tests().forEach(test -> appendTest(yaml, test));
        }
    }

    private static void appendCondition(
            StringBuilder yaml, RuleConditionSpec condition, int indentation) {
        String indent = " ".repeat(indentation);
        yaml.append(indent).append("operator: ").append(quoted(condition.operator())).append('\n');
        appendMap(yaml, indent, condition.parameters());
        if (!condition.operands().isEmpty()) {
            yaml.append(indent).append("operands:\n");
            condition.operands().forEach(operand -> {
                yaml.append(indent).append("  - operator: ").append(quoted(operand.operator())).append('\n');
                appendConditionBody(yaml, operand, indentation + 4);
            });
        }
    }

    private static void appendConditionBody(
            StringBuilder yaml, RuleConditionSpec condition, int indentation) {
        String indent = " ".repeat(indentation);
        appendMap(yaml, indent, condition.parameters());
        if (!condition.operands().isEmpty()) {
            yaml.append(indent).append("operands:\n");
            condition.operands().forEach(operand -> {
                yaml.append(indent).append("  - operator: ").append(quoted(operand.operator())).append('\n');
                appendConditionBody(yaml, operand, indentation + 4);
            });
        }
    }

    private static void appendMap(
            StringBuilder yaml, String indent, Map<String, String> values) {
        if (!values.isEmpty()) {
            yaml.append(indent).append("parameters:\n");
            values.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> yaml.append(indent).append("  ")
                            .append(entry.getKey()).append(": ").append(quoted(entry.getValue())).append('\n'));
        }
    }

    private static void appendAction(StringBuilder yaml, RuleActionSpec action) {
        yaml.append("      - type: ").append(quoted(action.type())).append('\n');
        if (!action.parameters().isEmpty()) {
            yaml.append("        parameters:\n");
            action.parameters().entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> yaml.append("          ").append(entry.getKey())
                            .append(": ").append(quoted(entry.getValue())).append('\n'));
        }
    }

    private static void appendTest(StringBuilder yaml, RuleTestCase test) {
        yaml.append("      - name: ").append(quoted(test.name())).append('\n')
                .append("        sender: ").append(quoted(test.sender().value())).append('\n')
                .append("        subject: ").append(quoted(test.subject())).append('\n')
                .append("        expect-match: ").append(test.expectedMatch()).append('\n');
    }

    private static String quoted(String value) {
        String escaped = value.replace("\\", "\\\\")
                .replace("\"", "\\\"").replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }
}
