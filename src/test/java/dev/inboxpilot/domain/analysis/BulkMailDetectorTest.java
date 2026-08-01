package dev.inboxpilot.domain.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.message.EmailAddress;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BulkMailDetector")
class BulkMailDetectorTest {

    @Test
    @DisplayName("ranks senders using unsubscribe, precedence, and sender-name signals")
    void detectsBulkSendersFromIndependentSignals() {
        EmailAddress newsletter = new EmailAddress("newsletter@example.com");
        EmailAddress notification = new EmailAddress("alerts@example.org");
        List<MailSignal> signals = List.of(
                new MailSignal(newsletter, Map.of("List-Unsubscribe", "<https://example.com/out>")),
                new MailSignal(newsletter, Map.of("Precedence", "bulk")),
                new MailSignal(notification, Map.of("Precedence", "list")));

        List<BulkMailSender> detected = new BulkMailDetector().detect(signals);

        assertThat(detected).extracting(BulkMailSender::sender)
                .containsExactly(newsletter, notification);
        assertThat(detected.get(0).signals())
                .containsExactly("list-unsubscribe", "precedence:bulk", "sender-pattern");
    }

    @Test
    @DisplayName("does not classify ordinary personal mail")
    void ignoresPersonalMail() {
        MailSignal signal = new MailSignal(
                new EmailAddress("friend@example.com"), Map.of("Precedence", "normal"));

        assertThat(new BulkMailDetector().detect(List.of(signal))).isEmpty();
    }
}
