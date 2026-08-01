package dev.inboxpilot.infrastructure.rules;

import dev.inboxpilot.domain.rules.RuleActionSpec;
import dev.inboxpilot.domain.rules.RuleConditionSpec;
import dev.inboxpilot.domain.rules.RuleConflictBehavior;
import dev.inboxpilot.domain.rules.RuleDefinition;
import dev.inboxpilot.domain.rules.RuleId;
import dev.inboxpilot.domain.rules.RuleMetadata;
import dev.inboxpilot.domain.rules.RuleSet;
import dev.inboxpilot.domain.rules.RuleTestCase;
import dev.inboxpilot.domain.message.EmailAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/** Safely parses and validates versioned YAML rule documents. */
public final class RuleYamlParser {

    private static final int MAX_ALIASES = 20;
    private static final int MAX_DOCUMENT_CHARACTERS = 1_000_000;
    private final Yaml yaml;

    public RuleYamlParser() {
        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(MAX_ALIASES);
        options.setCodePointLimit(MAX_DOCUMENT_CHARACTERS);
        options.setAllowDuplicateKeys(false);
        yaml = new Yaml(new SafeConstructor(options));
    }

    public RuleSet parse(String content) {
        try {
            Map<String, Object> document = map(yaml.load(content), "$", true);
            int version = integer(required(document, "version", "version"), "version");
            List<Object> rawRules = list(required(document, "rules", "rules"), "rules");
            List<RuleDefinition> rules = new ArrayList<>();
            for (int index = 0; index < rawRules.size(); index++) {
                rules.add(parseRule(rawRules.get(index), "rules[" + index + "]"));
            }
            return new RuleSet(version, rules);
        } catch (RuleValidationException exception) {
            throw exception;
        } catch (YAMLException | IllegalArgumentException exception) {
            throw new RuleValidationException("Invalid YAML rule document: " + exception.getMessage(), exception);
        }
    }

    private RuleDefinition parseRule(Object raw, String path) {
        Map<String, Object> rule = map(raw, path, false);
        RuleId id = new RuleId(text(required(rule, "id", path + ".id"), path + ".id"));
        int priority = integer(required(rule, "priority", path + ".priority"), path + ".priority");
        RuleConditionSpec condition = condition(required(rule, "match", path + ".match"), path + ".match");
        List<RuleActionSpec> actions = actions(required(rule, "actions", path + ".actions"), path + ".actions");
        String behavior = text(required(rule, "conflict-behavior", path + ".conflict-behavior"),
                path + ".conflict-behavior");
        RuleMetadata metadata = metadata(required(rule, "metadata", path + ".metadata"), path + ".metadata");
        List<RuleTestCase> tests = tests(rule.get("tests"), path + ".tests");
        return new RuleDefinition(id, priority, condition, actions,
                enumValue(behavior, path + ".conflict-behavior"), metadata, tests);
    }

    private RuleConditionSpec condition(Object raw, String path) {
        Map<String, Object> node = map(raw, path, false);
        String operator = text(required(node, "operator", path + ".operator"), path + ".operator");
        Map<String, String> parameters = stringMap(node.get("parameters"), path + ".parameters");
        List<RuleConditionSpec> operands = new ArrayList<>();
        List<Object> rawOperands = optionalList(node.get("operands"), path + ".operands");
        for (int index = 0; index < rawOperands.size(); index++) {
            operands.add(condition(rawOperands.get(index), path + ".operands[" + index + "]"));
        }
        return new RuleConditionSpec(operator, parameters, operands);
    }

    private List<RuleActionSpec> actions(Object raw, String path) {
        List<Object> entries = list(raw, path);
        List<RuleActionSpec> actions = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            String itemPath = path + "[" + index + "]";
            Map<String, Object> action = map(entries.get(index), itemPath, false);
            String type = text(required(action, "type", itemPath + ".type"), itemPath + ".type");
            actions.add(new RuleActionSpec(type, stringMap(action.get("parameters"), itemPath + ".parameters")));
        }
        return actions;
    }

    private RuleMetadata metadata(Object raw, String path) {
        Map<String, Object> metadata = map(raw, path, false);
        String description = text(required(metadata, "description", path + ".description"), path + ".description");
        String source = text(required(metadata, "source", path + ".source"), path + ".source");
        List<String> tags = optionalList(metadata.get("tags"), path + ".tags").stream()
                .map(value -> text(value, path + ".tags[]"))
                .toList();
        return new RuleMetadata(description, source, tags);
    }

    private List<RuleTestCase> tests(Object raw, String path) {
        List<Object> entries = optionalList(raw, path);
        List<RuleTestCase> tests = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            String itemPath = path + "[" + index + "]";
            Map<String, Object> test = map(entries.get(index), itemPath, false);
            String name = text(required(test, "name", itemPath + ".name"), itemPath + ".name");
            String sender = text(required(test, "sender", itemPath + ".sender"), itemPath + ".sender");
            String subject = text(required(test, "subject", itemPath + ".subject"), itemPath + ".subject");
            boolean expected = bool(required(test, "expect-match", itemPath + ".expect-match"),
                    itemPath + ".expect-match");
            tests.add(new RuleTestCase(name, new EmailAddress(sender), subject, expected));
        }
        return tests;
    }

    private static Map<String, String> stringMap(Object raw, String path) {
        if (raw == null) {
            return Map.of();
        }
        Map<String, Object> values = map(raw, path, false);
        Map<String, String> strings = new LinkedHashMap<>();
        values.forEach((key, value) -> strings.put(key, text(value, path + "." + key)));
        return strings;
    }

    private static Map<String, Object> map(Object raw, String path, boolean root) {
        if (!(raw instanceof Map<?, ?> values)) {
            String expectation = root ? "document" : "mapping";
            throw invalid(path, "expected a " + expectation);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((key, value) -> result.put(text(key, path + " key"), value));
        return result;
    }

    private static List<Object> list(Object raw, String path) {
        if (!(raw instanceof List<?> values)) {
            throw invalid(path, "expected a list");
        }
        return new ArrayList<>(values);
    }

    private static List<Object> optionalList(Object raw, String path) {
        return raw == null ? List.of() : list(raw, path);
    }

    private static Object required(Map<String, Object> values, String key, String path) {
        if (!values.containsKey(key) || values.get(key) == null) {
            throw invalid(path, "is required");
        }
        return values.get(key);
    }

    private static String text(Object raw, String path) {
        if (!(raw instanceof String value) || value.isBlank()) {
            throw invalid(path, "expected non-blank text");
        }
        return value;
    }

    private static int integer(Object raw, String path) {
        if (!(raw instanceof Number value)) {
            throw invalid(path, "expected an integer");
        }
        return value.intValue();
    }

    private static boolean bool(Object raw, String path) {
        if (!(raw instanceof Boolean value)) {
            throw invalid(path, "expected true or false");
        }
        return value;
    }

    private static RuleConflictBehavior enumValue(String value, String path) {
        try {
            return RuleConflictBehavior.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalid(path, "expected continue or stop");
        }
    }

    private static RuleValidationException invalid(String path, String detail) {
        return new RuleValidationException(path + ": " + detail);
    }
}
