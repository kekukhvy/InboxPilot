package dev.inboxpilot.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import dev.inboxpilot.application.port.MessageSnapshotStoreException;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessageSnapshotJsonStoreTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-08-01T10:00:00Z");
    private static final String SNAPSHOT_FILE = "messages.json";

    @TempDir
    Path directory;

    @Test
    void roundTripsEveryFieldClassificationReportsNeed() {
        MessageSnapshotJsonStore store = store();
        MailMessage message = new MailMessage(
                new MessageId("msg-1"), new EmailAddress("news@google.com"),
                "Security alert", RECEIVED_AT, List.of("Label_1", "INBOX"));

        store.write(List.of(message));

        assertThat(store.load()).containsExactly(message);
    }

    @Test
    void escapesSubjectsThatWouldOtherwiseBreakTheJsonDocument() {
        MessageSnapshotJsonStore store = store();
        MailMessage message = message("He said \"hi\"\n\tand\\left");

        store.write(List.of(message));

        assertThat(store.load()).containsExactly(message);
    }

    @Test
    void keepsAMessageWithoutASubject() {
        MessageSnapshotJsonStore store = store();
        MailMessage message = message("");

        store.write(List.of(message));

        assertThat(store.load()).containsExactly(message);
    }

    @Test
    void repeatedWritesOfTheSameMessagesProduceIdenticalBytes() throws IOException {
        MessageSnapshotJsonStore store = store();
        List<MailMessage> messages = List.of(message("Weekly digest"));

        store.write(messages);
        String first = Files.readString(directory.resolve(SNAPSHOT_FILE));
        store.write(messages);

        assertThat(Files.readString(directory.resolve(SNAPSHOT_FILE))).isEqualTo(first);
    }

    @Test
    void aBatchWriteReplacesThePreviousSnapshotSoFinishedWorkSurvives() {
        MessageSnapshotJsonStore store = store();
        MailMessage first = message("first");

        store.write(List.of(first));
        store.write(List.of(first, message("second")));

        assertThat(store.load()).hasSize(2);
    }

    /**
     * A real mailbox snapshot is megabytes: 22 907 messages produced a 5 MB file,
     * well past the parser's 3 MB default document ceiling. The reader must size
     * itself to the mailbox, not to the aggregate reports it was modelled on.
     */
    @Test
    void readsASnapshotLargerThanTheParserDefaultDocumentLimit() {
        MessageSnapshotJsonStore store = store();
        List<MailMessage> messages = largeSnapshot();

        store.write(messages);

        assertThat(store.load()).isEqualTo(messages);
    }

    @Test
    void missingSnapshotFailsWithAnInstructionToRunInventory() {
        assertThatExceptionOfType(MessageSnapshotStoreException.class)
                .isThrownBy(() -> store().load())
                .withMessageContaining("inventory");
    }

    @Test
    void unsupportedVersionIsRejectedRatherThanParsedOnAGuess() throws IOException {
        Files.writeString(
                directory.resolve(SNAPSHOT_FILE), "{\"version\":999,\"messages\":[]}");

        assertThatExceptionOfType(MessageSnapshotStoreException.class)
                .isThrownBy(() -> store().load())
                .withMessageContaining("999");
    }

    @Test
    void malformedSnapshotFailsWithAnInstructionToRunInventory() throws IOException {
        Files.writeString(directory.resolve(SNAPSHOT_FILE), "not a snapshot");

        assertThatExceptionOfType(MessageSnapshotStoreException.class)
                .isThrownBy(() -> store().load())
                .withMessageContaining("inventory");
    }

    private MessageSnapshotJsonStore store() {
        return new MessageSnapshotJsonStore(directory.resolve(SNAPSHOT_FILE));
    }

    /** Builds a snapshot comfortably past the 3 MB parser default. */
    private static List<MailMessage> largeSnapshot() {
        String subject = "Security alert ".repeat(20);
        return java.util.stream.IntStream.range(0, 25_000)
                .mapToObj(index -> new MailMessage(
                        new MessageId("msg-" + index),
                        new EmailAddress("sender" + index + "@example.com"),
                        subject + index,
                        RECEIVED_AT.plusSeconds(index),
                        List.of("Label_1", "INBOX")))
                .toList();
    }

    private static MailMessage message(String subject) {
        return new MailMessage(
                new MessageId("msg-1"), new EmailAddress("news@example.com"),
                subject, RECEIVED_AT, List.of());
    }
}
