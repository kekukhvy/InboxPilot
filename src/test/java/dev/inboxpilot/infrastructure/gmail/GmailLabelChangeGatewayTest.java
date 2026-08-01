package dev.inboxpilot.infrastructure.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.model.LabelBatchChange;
import dev.inboxpilot.domain.message.MessageId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GmailLabelChangeGateway")
class GmailLabelChangeGatewayTest {

    @Test
    @DisplayName("forwards only message IDs and explicit user-label changes")
    void appliesSafeBatch() {
        RecordingClient client = new RecordingClient();
        LabelBatchChange change = new LabelBatchChange(
                List.of(new MessageId("m1"), new MessageId("m2")),
                List.of("Label_Add"), List.of("Label_Remove"));

        new GmailLabelChangeGateway(client).apply(change);

        assertThat(client.messageIds).containsExactly("m1", "m2");
        assertThat(client.addLabelIds).containsExactly("Label_Add");
        assertThat(client.removeLabelIds).containsExactly("Label_Remove");
    }

    private static final class RecordingClient implements GmailApiClient {
        private List<String> messageIds;
        private List<String> addLabelIds;
        private List<String> removeLabelIds;

        @Override
        public List<GmailLabel> listLabels() {
            return List.of();
        }

        @Override
        public GmailMessagePage listMessageIds(String query, String pageToken) {
            return new GmailMessagePage(List.of(), null);
        }

        @Override
        public GmailMetadataBatch getMessageMetadata(List<String> ignored) {
            return new GmailMetadataBatch(List.of(), List.of());
        }

        @Override
        public void batchModifyLabels(
                List<String> messages, List<String> additions, List<String> removals) {
            messageIds = List.copyOf(messages);
            addLabelIds = List.copyOf(additions);
            removeLabelIds = List.copyOf(removals);
        }
    }
}
