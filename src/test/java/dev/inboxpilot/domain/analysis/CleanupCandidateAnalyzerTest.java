package dev.inboxpilot.domain.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MessageId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CleanupCandidateAnalyzer")
class CleanupCandidateAnalyzerTest {

    private static final Instant CUTOFF = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("reports stale cleanup candidates without defining an execution action")
    void reportsCleanupCandidates() {
        List<CleanupMessageProfile> messages = List.of(
                profile("otp", "Your verification code is 123456", false),
                profile("newsletter", "January digest", true),
                profile("alert", "Security alert resolved", false),
                profile("promo", "Limited time sale", false),
                new CleanupMessageProfile(
                        new MessageId("recent"), new EmailAddress("friend@example.com"),
                        "Dinner", CUTOFF.plusSeconds(1), false));

        CleanupCandidateReport report = new CleanupCandidateAnalyzer().analyze(messages, CUTOFF);

        assertThat(report.candidates()).extracting(CleanupCandidate::category)
                .containsExactly(
                        CleanupCategory.OBSOLETE_ALERT,
                        CleanupCategory.STALE_NEWSLETTER,
                        CleanupCategory.OTP,
                        CleanupCategory.OLD_PROMOTION);
        assertThat(CleanupCandidateAnalyzer.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().equals("delete"));
    }

    private static CleanupMessageProfile profile(String id, String subject, boolean bulkMail) {
        return new CleanupMessageProfile(
                new MessageId(id), new EmailAddress("sender@example.com"),
                subject, CUTOFF.minusSeconds(1), bulkMail);
    }
}
