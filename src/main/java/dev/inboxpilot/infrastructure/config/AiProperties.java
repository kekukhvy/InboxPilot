package dev.inboxpilot.infrastructure.config;

import dev.inboxpilot.domain.ai.AiPrivacyPolicy;

/** Optional AI data-sharing controls; every field is disabled by default. */
public record AiProperties(
        boolean enabled,
        boolean includeRedactedSubject,
        boolean includeLabelIds) {

    public AiPrivacyPolicy privacyPolicy() {
        return new AiPrivacyPolicy(enabled, includeRedactedSubject, includeLabelIds);
    }
}
