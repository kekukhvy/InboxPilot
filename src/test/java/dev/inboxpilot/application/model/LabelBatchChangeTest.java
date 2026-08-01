package dev.inboxpilot.application.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.inboxpilot.domain.message.MessageId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LabelBatchChange")
class LabelBatchChangeTest {

    @Test
    @DisplayName("rejects system labels that could alter mailbox state")
    void rejectsSystemLabels() {
        List<String> protectedLabels = List.of(
                "INBOX", "UNREAD", "STARRED", "IMPORTANT", "TRASH", "SPAM", "CATEGORY_UPDATES");

        protectedLabels.forEach(label -> assertThatThrownBy(() -> new LabelBatchChange(
                        List.of(new MessageId("m1")), List.of(label), List.of()))
                .isInstanceOf(IllegalArgumentException.class));
    }
}
