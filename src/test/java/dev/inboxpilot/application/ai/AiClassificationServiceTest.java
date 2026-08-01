package dev.inboxpilot.application.ai;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.port.AiClassifier;
import dev.inboxpilot.domain.ai.AiClassificationInput;
import dev.inboxpilot.domain.ai.AiClassificationSuggestion;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiClassificationService")
class AiClassificationServiceTest {

    @Test
    @DisplayName("works with interchangeable classifier implementations")
    void acceptsPluggableClassifiers() {
        AiClassificationInput input = new AiClassificationInput(
                "example.com", Optional.empty(), List.of());
        AiClassifier first = value -> suggestion(value, "First");
        AiClassifier second = value -> suggestion(value, "Second");

        assertThat(new AiClassificationService(first).classify(List.of(input)))
                .extracting(AiClassificationSuggestion::proposedLabel)
                .containsExactly("First");
        assertThat(new AiClassificationService(second).classify(List.of(input)))
                .extracting(AiClassificationSuggestion::proposedLabel)
                .containsExactly("Second");
    }

    private static AiClassificationSuggestion suggestion(
            AiClassificationInput input, String label) {
        return new AiClassificationSuggestion(
                input.senderDomain(), label, 0.9, "Test adapter rationale");
    }
}
