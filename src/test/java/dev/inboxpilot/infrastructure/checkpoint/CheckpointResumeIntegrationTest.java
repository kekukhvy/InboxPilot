package dev.inboxpilot.infrastructure.checkpoint;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.model.ScanCheckpoint;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Checkpoint resume integration")
class CheckpointResumeIntegrationTest {

    private static final String FINGERPRINT = "scan-fingerprint";
    private static final String FILE_NAME = "checkpoint.bin";
    private static final String FIRST_ID = "message-1";
    private static final String SECOND_ID = "message-2";
    private static final String SENDER = "sender@example.com";
    private static final String SUBJECT = "Subject";
    private static final Instant RECEIVED_AT = Instant.parse("2026-08-01T10:00:00Z");

    @TempDir
    Path directory;

    @Test
    @DisplayName("continues aggregate state from an interrupted scan")
    void resumesAggregateState() {
        FileCheckpointStore store = new FileCheckpointStore(directory.resolve(FILE_NAME));
        store.save(new ScanCheckpoint(FINGERPRINT, List.of(message(FIRST_ID))));

        List<MailMessage> resumed = new ArrayList<>(store.load(FINGERPRINT).orElseThrow().messages());
        resumed.add(message(SECOND_ID));
        store.save(new ScanCheckpoint(FINGERPRINT, resumed));

        assertThat(store.load(FINGERPRINT).orElseThrow().messages())
                .extracting(message -> message.id().value())
                .containsExactly(FIRST_ID, SECOND_ID);
    }

    private static MailMessage message(String id) {
        return new MailMessage(
                new MessageId(id), new EmailAddress(SENDER), SUBJECT, RECEIVED_AT, List.of());
    }
}
