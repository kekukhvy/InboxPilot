package dev.inboxpilot.domain.classification;

import java.util.List;

/** Disjoint review buckets for a complete classification plan. */
public record ClassificationAssessment(
        List<MessageLabelPlan> clean,
        List<MessageLabelPlan> ambiguous,
        List<MessageLabelPlan> unmatched) {

    public ClassificationAssessment {
        clean = List.copyOf(clean);
        ambiguous = List.copyOf(ambiguous);
        unmatched = List.copyOf(unmatched);
    }
}
