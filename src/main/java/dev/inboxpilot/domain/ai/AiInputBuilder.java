package dev.inboxpilot.domain.ai;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Applies local redaction and opt-in policy before any provider can see data. */
public final class AiInputBuilder {

    private static final Pattern EMAIL = Pattern.compile(
            "[A-Z0-9._%+-]+@[A-Z0-9.-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern LONG_NUMBER = Pattern.compile("\\b\\d{4,}\\b");
    private static final String EMAIL_REDACTION = "[email]";
    private static final String URL_REDACTION = "[url]";
    private static final String NUMBER_REDACTION = "[number]";

    public AiClassificationInput build(
            AiMessageMetadata metadata, AiPrivacyPolicy policy) {
        if (!policy.enabled()) {
            throw new IllegalStateException("AI classification requires explicit opt-in");
        }
        Optional<String> subject = policy.includeRedactedSubject()
                ? Optional.of(redact(metadata.subject()))
                : Optional.empty();
        List<String> labels = policy.includeLabelIds() ? metadata.labelIds() : List.of();
        return new AiClassificationInput(metadata.sender().domain(), subject, labels);
    }

    private static String redact(String value) {
        String redacted = EMAIL.matcher(value).replaceAll(EMAIL_REDACTION);
        redacted = URL.matcher(redacted).replaceAll(URL_REDACTION);
        return LONG_NUMBER.matcher(redacted).replaceAll(NUMBER_REDACTION);
    }
}
