package dev.inboxpilot.domain.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MessageId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DeletionCandidateReporter")
class DeletionCandidateReporterTest {

    @Test
    @DisplayName("combines stale and transient candidates without an execution operation")
    void reportsCandidatesOnly() {
        Instant receivedAt = Instant.parse("2026-01-01T00:00:00Z");
        StaleNewsletterGroup newsletters = new StaleNewsletterGroup(
                new EmailAddress("news@example.com"), List.of(new MessageId("newsletter")),
                receivedAt, receivedAt);
        TransientMessageCandidate transientCandidate = new TransientMessageCandidate(
                new MessageId("otp"), TransientMessageCategory.OTP, "subject matched otp");

        DeletionCandidateReport report = new DeletionCandidateReporter()
                .create(List.of(newsletters), List.of(transientCandidate));

        assertThat(report.candidates()).extracting(DeletionCandidate::category)
                .containsExactly(DeletionCandidateCategory.STALE_NEWSLETTER, DeletionCandidateCategory.OTP);
        assertThat(DeletionCandidateReporter.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().contains("delete"));
    }
}
