package dev.inboxpilot.domain.ai;

import java.util.Objects;

/** Provider-neutral label suggestion with confidence and review rationale. */
public record AiClassificationSuggestion(
        String senderDomain,
        String proposedLabel,
        double confidence,
        String rationale) {

    public AiClassificationSuggestion {
        Objects.requireNonNull(senderDomain, "senderDomain");
        Objects.requireNonNull(proposedLabel, "proposedLabel");
        Objects.requireNonNull(rationale, "rationale");
        if (proposedLabel.isBlank()) {
            throw new IllegalArgumentException("AI proposed label must not be blank");
        }
        if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("AI confidence must be between 0 and 1");
        }
    }
}
