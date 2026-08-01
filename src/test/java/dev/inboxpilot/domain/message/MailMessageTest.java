package dev.inboxpilot.domain.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MailMessage")
class MailMessageTest {

    private static final MessageId MESSAGE_ID = new MessageId("msg-1");
    private static final EmailAddress SENDER = new EmailAddress("news@example.com");
    private static final String SUBJECT = "Weekly digest";
    private static final Instant RECEIVED_AT = Instant.parse("2026-08-01T10:15:30Z");
    private static final String NEWSLETTER_LABEL = "Newsletters";
    private static final String ABSENT_LABEL = "Receipts";

    @Test
    @DisplayName("reports a label it carries")
    void reportsPresentLabel() {
        assertThat(messageWithLabels(List.of(NEWSLETTER_LABEL)).hasLabel(NEWSLETTER_LABEL)).isTrue();
    }

    @Test
    @DisplayName("reports a label it does not carry")
    void reportsAbsentLabel() {
        assertThat(messageWithLabels(List.of(NEWSLETTER_LABEL)).hasLabel(ABSENT_LABEL)).isFalse();
    }

    @Test
    @DisplayName("keeps its labels immutable")
    void keepsLabelsImmutable() {
        assertThatThrownBy(() -> messageWithLabels(List.of(NEWSLETTER_LABEL)).labels().add(ABSENT_LABEL))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("is unaffected by later changes to the list it was built from")
    void copiesLabelsDefensively() {
        List<String> mutableLabels = new ArrayList<>(List.of(NEWSLETTER_LABEL));
        MailMessage message = messageWithLabels(mutableLabels);

        mutableLabels.add(ABSENT_LABEL);

        assertThat(message.hasLabel(ABSENT_LABEL)).isFalse();
        assertThat(message.labels()).containsExactly(NEWSLETTER_LABEL);
    }

    @Test
    @DisplayName("rejects a missing sender")
    void rejectsNullSender() {
        assertThatThrownBy(() -> new MailMessage(
                MESSAGE_ID, null, SUBJECT, RECEIVED_AT, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sender");
    }

    private static MailMessage messageWithLabels(List<String> labels) {
        return new MailMessage(MESSAGE_ID, SENDER, SUBJECT, RECEIVED_AT, labels);
    }
}
