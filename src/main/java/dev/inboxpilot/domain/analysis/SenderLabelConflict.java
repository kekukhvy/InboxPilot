package dev.inboxpilot.domain.analysis;

import dev.inboxpilot.domain.message.EmailAddress;
import java.util.List;

/** Incompatible labels observed for one sender with representative subjects. */
public record SenderLabelConflict(
        EmailAddress sender,
        List<String> conflictingLabels,
        List<String> representativeSubjects) {

    public SenderLabelConflict {
        conflictingLabels = List.copyOf(conflictingLabels);
        representativeSubjects = List.copyOf(representativeSubjects);
    }
}
