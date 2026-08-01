package dev.inboxpilot.infrastructure.checkpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import dev.inboxpilot.application.model.ScanCheckpoint;
import dev.inboxpilot.application.port.CheckpointStoreException;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("FileCheckpointStore")
class FileCheckpointStoreTest {

    private static final String FINGERPRINT = "fingerprint-a";
    private static final String OTHER_FINGERPRINT = "fingerprint-b";
    private static final String MESSAGE_ID = "message-1";
    private static final String SENDER = "sender@example.com";
    private static final String SUBJECT = "Subject";
    private static final String LABEL = "INBOX";
    private static final Instant RECEIVED_AT = Instant.parse("2026-08-01T10:00:00Z");
    private static final String FILE_NAME = "checkpoint.bin";

    @TempDir
    Path directory;

    @Test
    @DisplayName("round trips progress and aggregate source messages")
    void roundTripsCheckpoint() {
        FileCheckpointStore store = new FileCheckpointStore(directory.resolve(FILE_NAME));
        ScanCheckpoint checkpoint = new ScanCheckpoint(FINGERPRINT, RECEIVED_AT, List.of(message()));

        store.save(checkpoint);

        assertThat(store.load(FINGERPRINT)).contains(checkpoint);
    }

    @Test
    @DisplayName("rejects resume when configuration fingerprint changed")
    void rejectsIncompatibleResume() {
        FileCheckpointStore store = new FileCheckpointStore(directory.resolve(FILE_NAME));
        store.save(new ScanCheckpoint(FINGERPRINT, RECEIVED_AT, List.of(message())));

        assertThatExceptionOfType(CheckpointStoreException.class)
                .isThrownBy(() -> store.load(OTHER_FINGERPRINT))
                .withMessageContaining("incompatible");
    }

    @Test
    @DisplayName("reset explicitly removes saved progress")
    void resetsCheckpoint() {
        Path file = directory.resolve(FILE_NAME);
        FileCheckpointStore store = new FileCheckpointStore(file);
        store.save(new ScanCheckpoint(FINGERPRINT, RECEIVED_AT, List.of(message())));

        store.reset();

        assertThat(Files.exists(file)).isFalse();
        assertThat(store.load(FINGERPRINT)).isEmpty();
    }

    private static MailMessage message() {
        return new MailMessage(
                new MessageId(MESSAGE_ID), new EmailAddress(SENDER), SUBJECT,
                RECEIVED_AT, List.of(LABEL));
    }
}
