package dev.inboxpilot.infrastructure.checkpoint;

import dev.inboxpilot.application.model.ScanCheckpoint;
import dev.inboxpilot.application.port.CheckpointStore;
import dev.inboxpilot.application.port.CheckpointStoreException;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.infrastructure.config.InboxPilotProperties;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Versioned binary checkpoint store using atomic same-directory replacement. */
@Component
public class FileCheckpointStore implements CheckpointStore {

    private static final int FORMAT_VERSION = 1;
    private static final String TEMP_SUFFIX = ".tmp";
    private static final String READ_FAILURE = "Checkpoint could not be read: ";
    private static final String WRITE_FAILURE = "Checkpoint could not be written atomically: ";
    private static final String RESET_FAILURE = "Checkpoint could not be reset: ";
    private static final String VERSION_FAILURE = "Unsupported checkpoint version: ";
    private static final String FINGERPRINT_FAILURE =
            "Checkpoint is incompatible with the current configuration/query";

    private final Path file;

    @Autowired
    public FileCheckpointStore(InboxPilotProperties properties) {
        this(properties.checkpoints().file());
    }

    FileCheckpointStore(Path file) {
        this.file = file;
    }

    @Override
    public Optional<ScanCheckpoint> load(String expectedFingerprint) {
        if (Files.notExists(file)) {
            return Optional.empty();
        }
        try (DataInputStream input = new DataInputStream(Files.newInputStream(file))) {
            validateVersion(input.readInt());
            String fingerprint = input.readUTF();
            validateFingerprint(expectedFingerprint, fingerprint);
            return Optional.of(new ScanCheckpoint(fingerprint, readMessages(input)));
        } catch (IOException exception) {
            throw new CheckpointStoreException(READ_FAILURE + file, exception);
        }
    }

    @Override
    public void save(ScanCheckpoint checkpoint) {
        Path temporaryFile = file.resolveSibling(file.getFileName() + TEMP_SUFFIX);
        try {
            createParentDirectory();
            write(temporaryFile, checkpoint);
            Files.move(temporaryFile, file,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            throw new CheckpointStoreException(WRITE_FAILURE + file, exception);
        } catch (IOException exception) {
            throw new CheckpointStoreException(WRITE_FAILURE + file, exception);
        }
    }

    @Override
    public void reset() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new CheckpointStoreException(RESET_FAILURE + file, exception);
        }
    }

    private void createParentDirectory() throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static void write(Path target, ScanCheckpoint checkpoint) throws IOException {
        try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(target))) {
            output.writeInt(FORMAT_VERSION);
            output.writeUTF(checkpoint.fingerprint());
            output.writeInt(checkpoint.messages().size());
            for (MailMessage message : checkpoint.messages()) {
                writeMessage(output, message);
            }
        }
    }

    private static void writeMessage(DataOutputStream output, MailMessage message)
            throws IOException {
        output.writeUTF(message.id().value());
        output.writeUTF(message.sender().value());
        output.writeUTF(message.subject());
        output.writeLong(message.receivedAt().toEpochMilli());
        output.writeInt(message.labels().size());
        for (String label : message.labels()) {
            output.writeUTF(label);
        }
    }

    private static List<MailMessage> readMessages(DataInputStream input) throws IOException {
        int messageCount = input.readInt();
        List<MailMessage> messages = new ArrayList<>(messageCount);
        for (int messageIndex = 0; messageIndex < messageCount; messageIndex++) {
            messages.add(readMessage(input));
        }
        return List.copyOf(messages);
    }

    private static MailMessage readMessage(DataInputStream input) throws IOException {
        MessageId id = new MessageId(input.readUTF());
        EmailAddress sender = new EmailAddress(input.readUTF());
        String subject = input.readUTF();
        Instant receivedAt = Instant.ofEpochMilli(input.readLong());
        int labelCount = input.readInt();
        List<String> labels = new ArrayList<>(labelCount);
        for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
            labels.add(input.readUTF());
        }
        return new MailMessage(id, sender, subject, receivedAt, labels);
    }

    private static void validateVersion(int version) {
        if (version != FORMAT_VERSION) {
            throw new CheckpointStoreException(VERSION_FAILURE + version);
        }
    }

    private static void validateFingerprint(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new CheckpointStoreException(FINGERPRINT_FAILURE);
        }
    }
}
