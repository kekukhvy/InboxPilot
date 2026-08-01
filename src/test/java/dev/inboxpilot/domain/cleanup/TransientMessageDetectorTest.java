package dev.inboxpilot.domain.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TransientMessageDetector")
class TransientMessageDetectorTest {

    @Test
    @DisplayName("identifies OTP, login, delivery, and transient status messages")
    void identifiesTransientCategories() {
        List<MailMessage> messages = List.of(
                message("otp", "Your verification code is 123456"),
                message("login", "New login alert"),
                message("delivery", "Your parcel was delivered"),
                message("status", "Job completed successfully"),
                message("personal", "Dinner tomorrow?"));

        List<TransientMessageCandidate> candidates = new TransientMessageDetector().detect(messages);

        assertThat(candidates).extracting(TransientMessageCandidate::category)
                .containsExactly(
                        TransientMessageCategory.DELIVERY_STATUS,
                        TransientMessageCategory.LOGIN_ALERT,
                        TransientMessageCategory.OTP,
                        TransientMessageCategory.TRANSIENT_NOTIFICATION);
        assertThat(candidates).allMatch(candidate -> !candidate.reason().isBlank());
    }

    private static MailMessage message(String id, String subject) {
        return new MailMessage(
                new MessageId(id), new EmailAddress("sender@example.com"), subject,
                Instant.parse("2026-01-01T00:00:00Z"), List.of());
    }
}
