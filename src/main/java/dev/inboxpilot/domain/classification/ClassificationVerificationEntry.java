package dev.inboxpilot.domain.classification;

import dev.inboxpilot.domain.message.MessageId;
import java.util.List;

/** Expected and actual label state with exact discrepancies. */
public record ClassificationVerificationEntry(
        MessageId messageId,
        List<String> expectedLabels,
        List<String> actualLabels,
        List<String> missingLabels,
        List<String> unexpectedLabels,
        ClassificationVerificationStatus status) {

    public ClassificationVerificationEntry {
        expectedLabels = List.copyOf(expectedLabels);
        actualLabels = List.copyOf(actualLabels);
        missingLabels = List.copyOf(missingLabels);
        unexpectedLabels = List.copyOf(unexpectedLabels);
    }
}
