package dev.inboxpilot.application.analysis;

import dev.inboxpilot.domain.message.EmailAddress;
import java.util.List;

/** Sender carrying multiple user labels and requiring review. */
public record AnalysisLabelConflict(
        EmailAddress sender, List<String> labels, int messageCount) {

    public AnalysisLabelConflict {
        labels = List.copyOf(labels);
    }
}
