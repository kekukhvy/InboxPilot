package dev.inboxpilot.domain.ai;

import java.util.List;
import java.util.Optional;

/** Minimal provider-neutral payload allowed to leave the local process. */
public record AiClassificationInput(
        String senderDomain,
        Optional<String> redactedSubject,
        List<String> labelIds) {

    public AiClassificationInput {
        redactedSubject = redactedSubject == null ? Optional.empty() : redactedSubject;
        labelIds = List.copyOf(labelIds);
    }
}
