package dev.inboxpilot.application.port;

import dev.inboxpilot.domain.ai.AiClassificationInput;
import dev.inboxpilot.domain.ai.AiClassificationSuggestion;

/** Pluggable boundary implemented by remote or local AI adapters. */
public interface AiClassifier {

    AiClassificationSuggestion classify(AiClassificationInput input);
}
