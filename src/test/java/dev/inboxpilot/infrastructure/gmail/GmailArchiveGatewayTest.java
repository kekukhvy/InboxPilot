package dev.inboxpilot.infrastructure.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.message.MessageId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GmailArchiveGateway")
class GmailArchiveGatewayTest {

    @Test
    @DisplayName("archives only by removing INBOX")
    void removesOnlyInbox() {
        RecordingClient client = new RecordingClient();

        new GmailArchiveGateway(client).archive(List.of(new MessageId("m1")));

        assertThat(client.messageIds).containsExactly("m1");
        assertThat(client.addLabelIds).isEmpty();
        assertThat(client.removeLabelIds).containsExactly("INBOX");
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
        public GmailMetadataBatch getMessageMetadata(List<String> messageIds) {
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
