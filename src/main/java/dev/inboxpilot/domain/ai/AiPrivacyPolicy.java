package dev.inboxpilot.domain.ai;

/** Explicit local consent controlling optional AI input fields. */
public record AiPrivacyPolicy(
        boolean enabled,
        boolean includeRedactedSubject,
        boolean includeLabelIds) {

    public static AiPrivacyPolicy disabled() {
        return new AiPrivacyPolicy(false, false, false);
    }
}
