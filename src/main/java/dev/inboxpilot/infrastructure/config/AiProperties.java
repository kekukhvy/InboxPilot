package dev.inboxpilot.infrastructure.config;

import dev.inboxpilot.domain.ai.AiPrivacyPolicy;

/** Optional AI data-sharing controls; every field is disabled by default. */
public record AiProperties(
        boolean enabled,
        boolean includeRedactedSubject,
        boolean includeLabelIds,
        double minimumAutomaticConfidence) {

    public AiProperties {
        if (!Double.isFinite(minimumAutomaticConfidence)
                || minimumAutomaticConfidence < 0.0
                || minimumAutomaticConfidence > 1.0) {
            throw new IllegalArgumentException("AI confidence threshold must be between 0 and 1");
        }
    }

    public AiPrivacyPolicy privacyPolicy() {
        return new AiPrivacyPolicy(enabled, includeRedactedSubject, includeLabelIds);
    }
}
