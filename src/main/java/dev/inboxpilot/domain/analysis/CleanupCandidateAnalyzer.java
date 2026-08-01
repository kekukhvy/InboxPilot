package dev.inboxpilot.domain.analysis;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Produces read-only cleanup candidates from old message metadata. */
public final class CleanupCandidateAnalyzer {

    private static final List<String> OTP_TERMS = List.of("verification code", "one-time", " otp");
    private static final List<String> ALERT_TERMS = List.of("alert", "notification", "reminder");
    private static final List<String> PROMOTION_TERMS =
            List.of("sale", "discount", "offer", "promotion", "limited time");

    public CleanupCandidateReport analyze(
            Collection<CleanupMessageProfile> messages, Instant staleBefore) {
        List<CleanupCandidate> candidates = messages.stream()
                .filter(message -> message.receivedAt().isBefore(staleBefore))
                .map(CleanupCandidateAnalyzer::classify)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(CleanupCandidate::receivedAt)
                        .thenComparing(candidate -> candidate.id().value()))
                .toList();
        return new CleanupCandidateReport(candidates);
    }

    private static Optional<CleanupCandidate> classify(CleanupMessageProfile message) {
        String subject = message.subject().toLowerCase(Locale.ROOT);
        CleanupCategory category = category(subject, message.bulkMail());
        if (category == null) {
            return Optional.empty();
        }
        return Optional.of(new CleanupCandidate(
                message.id(), message.sender(), message.subject(), message.receivedAt(), category));
    }

    private static CleanupCategory category(String subject, boolean bulkMail) {
        if (containsAny(subject, OTP_TERMS)) {
            return CleanupCategory.OTP;
        }
        if (bulkMail) {
            return CleanupCategory.STALE_NEWSLETTER;
        }
        if (containsAny(subject, ALERT_TERMS)) {
            return CleanupCategory.OBSOLETE_ALERT;
        }
        if (containsAny(subject, PROMOTION_TERMS)) {
            return CleanupCategory.OLD_PROMOTION;
        }
        return null;
    }

    private static boolean containsAny(String value, List<String> terms) {
        return terms.stream().anyMatch(value::contains);
    }
}
