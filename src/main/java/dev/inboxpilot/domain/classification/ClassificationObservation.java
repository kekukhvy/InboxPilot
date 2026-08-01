package dev.inboxpilot.domain.classification;

import java.util.List;

/** Actual label state and outcome observed for one message execution. */
public record ClassificationObservation(
        ClassificationAuditResult result,
        List<String> newLabels) {

    public ClassificationObservation {
        newLabels = newLabels.stream().sorted().toList();
    }
}
