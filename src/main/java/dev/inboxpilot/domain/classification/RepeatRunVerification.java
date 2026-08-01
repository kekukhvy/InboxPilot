package dev.inboxpilot.domain.classification;

import dev.inboxpilot.domain.message.MessageId;
import java.util.List;

/** Evidence that re-evaluation would or would not make additional changes. */
public record RepeatRunVerification(
        int evaluatedMessages,
        List<MessageId> additionalChangeMessageIds) {

    public RepeatRunVerification {
        additionalChangeMessageIds = List.copyOf(additionalChangeMessageIds);
    }

    public boolean idempotent() {
        return additionalChangeMessageIds.isEmpty();
    }
}
