package dev.inboxpilot.domain.cleanup;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Combines cleanup detections into a non-executable deletion candidate report. */
public final class DeletionCandidateReporter {

    public DeletionCandidateReport create(
            Collection<StaleNewsletterGroup> newsletters,
            Collection<TransientMessageCandidate> transientMessages) {
        Stream<DeletionCandidate> stale = newsletters.stream().flatMap(group -> group.messageIds().stream()
                .map(messageId -> new DeletionCandidate(
                        messageId,
                        DeletionCandidateCategory.STALE_NEWSLETTER,
                        "newsletter from " + group.sender().value() + " is older than the configured threshold")));
        Stream<DeletionCandidate> transientCandidates = transientMessages.stream()
                .map(candidate -> new DeletionCandidate(
                        candidate.messageId(), category(candidate.category()), candidate.reason()));
        List<DeletionCandidate> candidates = Stream.concat(stale, transientCandidates)
                .sorted(Comparator.comparing(candidate -> candidate.messageId().value()))
                .toList();
        return new DeletionCandidateReport(candidates);
    }

    private static DeletionCandidateCategory category(TransientMessageCategory category) {
        return DeletionCandidateCategory.valueOf(category.name());
    }
}
