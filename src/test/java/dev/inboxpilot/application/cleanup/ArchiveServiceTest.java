package dev.inboxpilot.application.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.model.ArchiveMode;
import dev.inboxpilot.application.model.ArchivePlan;
import dev.inboxpilot.application.port.ArchiveGateway;
import dev.inboxpilot.domain.message.MessageId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ArchiveService")
class ArchiveServiceTest {

    @Test
    @DisplayName("defaults to preview and archives only in explicit execute mode")
    void defaultsToDryRun() {
        RecordingArchiveGateway gateway = new RecordingArchiveGateway();
        ArchiveService service = new ArchiveService(gateway);
        MessageId messageId = new MessageId("m1");

        ArchivePlan preview = service.run(List.of(messageId));

        assertThat(preview.messageIds()).containsExactly(messageId);
        assertThat(gateway.archived).isEmpty();

        service.run(List.of(messageId), ArchiveMode.EXECUTE);

        assertThat(gateway.archived).containsExactly(messageId);
    }

    private static final class RecordingArchiveGateway implements ArchiveGateway {
        private final List<MessageId> archived = new ArrayList<>();

        @Override
        public void archive(List<MessageId> messageIds) {
            archived.addAll(messageIds);
        }
    }
}
