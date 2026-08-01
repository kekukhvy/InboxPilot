package dev.inboxpilot.domain.rules;

import dev.inboxpilot.domain.message.MailMessage;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Compiles supported leaf condition specifications into reusable matchers. */
public final class LeafConditionCompiler {

    private static final String VALUE = "value";
    private static final String IGNORE_CASE = "ignore-case";

    public MessageCondition compile(RuleConditionSpec specification) {
        if (!specification.operands().isEmpty()) {
            throw new IllegalArgumentException("Leaf condition must not contain operands");
        }
        String value = requiredValue(specification.parameters());
        boolean ignoreCase = booleanParameter(specification.parameters(), IGNORE_CASE, false);
        return switch (specification.operator()) {
            case "sender-exact" -> exactSender(value, ignoreCase);
            case "sender-domain" -> exactDomain(value, ignoreCase);
            case "sender-domain-suffix" -> domainSuffix(value, ignoreCase);
            case "subject-contains" -> subjectContains(value, ignoreCase);
            case "subject-regex" -> subjectRegex(value, ignoreCase);
            default -> throw new IllegalArgumentException(
                    "Unsupported leaf condition operator: " + specification.operator());
        };
    }

    private static MessageCondition exactSender(String expected, boolean ignoreCase) {
        String normalized = normalize(expected, ignoreCase);
        return message -> normalize(message.sender().value(), ignoreCase).equals(normalized);
    }

    private static MessageCondition exactDomain(String expected, boolean ignoreCase) {
        String normalized = normalize(expected, ignoreCase);
        return message -> normalize(message.sender().domain(), ignoreCase).equals(normalized);
    }

    private static MessageCondition domainSuffix(String expected, boolean ignoreCase) {
        String normalized = normalize(expected, ignoreCase);
        return message -> {
            String domain = normalize(message.sender().domain(), ignoreCase);
            return domain.equals(normalized) || domain.endsWith("." + normalized);
        };
    }

    private static MessageCondition subjectContains(String expected, boolean ignoreCase) {
        String normalized = normalize(expected, ignoreCase);
        return message -> normalize(message.subject(), ignoreCase).contains(normalized);
    }

    private static MessageCondition subjectRegex(String expression, boolean ignoreCase) {
        int flags = ignoreCase ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
        try {
            Pattern pattern = Pattern.compile(expression, flags);
            return message -> pattern.matcher(message.subject()).find();
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("Invalid subject regex: " + exception.getDescription(), exception);
        }
    }

    private static String requiredValue(Map<String, String> parameters) {
        String value = parameters.get(VALUE);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Condition parameter 'value' is required");
        }
        return value;
    }

    private static boolean booleanParameter(
            Map<String, String> parameters, String name, boolean defaultValue) {
        String value = parameters.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("Condition parameter '" + name + "' must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    private static String normalize(String value, boolean ignoreCase) {
        return ignoreCase ? value.toLowerCase(Locale.ROOT) : value;
    }
}
