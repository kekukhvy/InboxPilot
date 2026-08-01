package dev.inboxpilot.application.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.model.MailboxLabel;
import dev.inboxpilot.application.port.MailboxGateway;
import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");
    private static final Duration LOOKBACK = Duration.ofDays(30);
    private static final int MAXIMUM_MESSAGES = 100;
    private static final String MESSAGE_ID = "message-1";
    private static final String SENDER = "sender@example.com";
    private static final String SUBJECT = "Subject";
    private static final String USER_LABEL_ID = "Label_user";
    private static final String USER_LABEL_NAME = "Work";
    private static final String UNUSED_LABEL_ID = "Label_unused";
    private static final String SYSTEM_LABEL_ID = "INBOX";

    @Test
    void analyzesUnclassifiedMessagesAndUserLabelStructure() {
        MessageSource source = since -> List.of(message());
        MailboxGateway gateway = new StubMailboxGateway(List.of(
                new MailboxLabel(USER_LABEL_ID, USER_LABEL_NAME),
                new MailboxLabel(UNUSED_LABEL_ID, USER_LABEL_NAME),
                new MailboxLabel(SYSTEM_LABEL_ID, SYSTEM_LABEL_ID)));
        AnalysisService service = new AnalysisService(
                source, gateway, LOOKBACK, MAXIMUM_MESSAGES,
                Clock.fixed(NOW, ZoneOffset.UTC));

        AnalysisRunResult result = service.run();

        assertThat(result).isEqualTo(new AnalysisRunResult(1, 1, 1, 2, 1));
    }

    private static MailMessage message() {
        return new MailMessage(
                new MessageId(MESSAGE_ID), new EmailAddress(SENDER), SUBJECT,
                NOW.minusSeconds(1), List.of(SYSTEM_LABEL_ID));
    }

    private record StubMailboxGateway(List<MailboxLabel> labels) implements MailboxGateway {
        @Override
        public List<MailboxLabel> listLabels() {
            return labels;
        }

        @Override
        public List<MessageId> listMessageIds(String query) {
            return List.of();
        }
    }
}
