package dev.inboxpilot.application.ai;

import dev.inboxpilot.application.port.AiClassifier;
import dev.inboxpilot.domain.ai.AiClassificationInput;
import dev.inboxpilot.domain.ai.AiClassificationSuggestion;
import java.util.Collection;
import java.util.List;

/** Delegates privacy-filtered inputs through the configured classifier port. */
public final class AiClassificationService {

    private final AiClassifier classifier;

    public AiClassificationService(AiClassifier classifier) {
        this.classifier = classifier;
    }

    public List<AiClassificationSuggestion> classify(
            Collection<AiClassificationInput> inputs) {
        return inputs.stream().map(classifier::classify).toList();
    }
}
