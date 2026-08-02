package dev.inboxpilot.infrastructure.report;

import dev.inboxpilot.application.port.MessageSnapshotStore;
import dev.inboxpilot.application.port.MessageSnapshotStoreException;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.infrastructure.config.InboxPilotProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Stores the message-level inventory snapshot as versioned JSON.
 *
 * <p>Written by inventory at every completed batch boundary and read by the
 * classification dry run, which must never fall back to the mailbox
 * (issue #151). The file is replaced atomically, so an interrupted write cannot
 * leave a half-written snapshot behind.
 *
 * <p>The {@code version} field is what makes the format evolvable: a snapshot
 * from an unknown version is rejected with the same actionable instruction as a
 * missing one — rerun inventory — rather than being parsed on a guess.
 */
@Component
public final class MessageSnapshotJsonStore implements MessageSnapshotStore {

    static final int FORMAT_VERSION = 1;

    /**
     * Ceiling on a readable snapshot, in code points. Roughly 512 MB — about two
     * million messages at observed sizes — chosen to be far beyond any real
     * mailbox while still refusing a runaway or corrupt file.
     */
    private static final int MAXIMUM_SNAPSHOT_CODE_POINTS = 512 * 1024 * 1024;

    private static final String SNAPSHOT_FILE = "messages.json";
    private static final String TEMP_SUFFIX = ".tmp";
    private static final String RERUN_INSTRUCTION =
            " Run 'inventory' to build a message snapshot before classifying.";
    private static final String MISSING_FAILURE =
            "No message snapshot found at %s."
                    + " Existing aggregate-only inventory.json cannot be classified."
                    + RERUN_INSTRUCTION;
    private static final String VERSION_FAILURE =
            "Message snapshot at %s has unsupported version %s; expected " + FORMAT_VERSION + "."
                    + RERUN_INSTRUCTION;
    private static final String READ_FAILURE = "Message snapshot could not be read: ";
    private static final String WRITE_FAILURE = "Message snapshot could not be written: ";

    private static final String VERSION_FIELD = "version";
    private static final String MESSAGES_FIELD = "messages";
    private static final String ID_FIELD = "id";
    private static final String SENDER_FIELD = "sender";
    private static final String SUBJECT_FIELD = "subject";
    private static final String RECEIVED_AT_FIELD = "receivedAt";
    private static final String LABELS_FIELD = "labels";

    private final Path path;
    private final Yaml yaml;

    @Autowired
    public MessageSnapshotJsonStore(InboxPilotProperties properties) {
        this(properties.reports().outputDirectory().resolve(SNAPSHOT_FILE));
    }

    MessageSnapshotJsonStore(Path path) {
        this.path = path;
        this.yaml = new Yaml(new SafeConstructor(loaderOptions()));
    }

    /**
     * The snapshot is sized by the mailbox, not by a document convention: a
     * 22 000-message scan already produces about 5 MB, past the parser's 3 MB
     * default ceiling. The limit is raised rather than removed, so a corrupt
     * file still fails instead of exhausting memory.
     */
    private static LoaderOptions loaderOptions() {
        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(MAXIMUM_SNAPSHOT_CODE_POINTS);
        return options;
    }

    @Override
    public void write(List<MailMessage> messages) {
        Path temporaryFile = path.resolveSibling(path.getFileName() + TEMP_SUFFIX);
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.writeString(temporaryFile, render(messages), StandardCharsets.UTF_8);
            move(temporaryFile);
        } catch (IOException exception) {
            throw new MessageSnapshotStoreException(WRITE_FAILURE + path, exception);
        }
    }

    @Override
    public List<MailMessage> load() {
        if (Files.notExists(path)) {
            throw new MessageSnapshotStoreException(MISSING_FAILURE.formatted(path));
        }
        Map<?, ?> document = document();
        validateVersion(document.get(VERSION_FIELD));
        return messages(document.get(MESSAGES_FIELD));
    }

    private Map<?, ?> document() {
        try {
            Object raw = yaml.load(Files.readString(path));
            if (raw instanceof Map<?, ?> value) {
                return value;
            }
            throw new MessageSnapshotStoreException(READ_FAILURE + path + RERUN_INSTRUCTION);
        } catch (IOException | RuntimeException exception) {
            throw snapshotFailure(exception);
        }
    }

    private MessageSnapshotStoreException snapshotFailure(Exception exception) {
        if (exception instanceof MessageSnapshotStoreException failure) {
            return failure;
        }
        return new MessageSnapshotStoreException(
                READ_FAILURE + path + RERUN_INSTRUCTION, exception);
    }

    private void move(Path temporaryFile) throws IOException {
        try {
            Files.move(temporaryFile, path,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void validateVersion(Object raw) {
        if (!(raw instanceof Number version) || version.intValue() != FORMAT_VERSION) {
            throw new MessageSnapshotStoreException(VERSION_FAILURE.formatted(path, raw));
        }
    }

    private List<MailMessage> messages(Object raw) {
        if (!(raw instanceof List<?> values)) {
            throw new MessageSnapshotStoreException(READ_FAILURE + path + RERUN_INSTRUCTION);
        }
        return values.stream().map(this::message).toList();
    }

    private MailMessage message(Object raw) {
        if (!(raw instanceof Map<?, ?> value)) {
            throw new MessageSnapshotStoreException(READ_FAILURE + path + RERUN_INSTRUCTION);
        }
        return new MailMessage(
                new MessageId(text(value.get(ID_FIELD))),
                new EmailAddress(text(value.get(SENDER_FIELD))),
                subject(value.get(SUBJECT_FIELD)),
                Instant.parse(text(value.get(RECEIVED_AT_FIELD))),
                labels(value.get(LABELS_FIELD)));
    }

    private List<String> labels(Object raw) {
        if (!(raw instanceof List<?> values)) {
            throw new MessageSnapshotStoreException(READ_FAILURE + path + RERUN_INSTRUCTION);
        }
        return values.stream().map(this::text).toList();
    }

    private String text(Object raw) {
        if (raw instanceof String value && !value.isBlank()) {
            return value;
        }
        throw new MessageSnapshotStoreException(READ_FAILURE + path + RERUN_INSTRUCTION);
    }

    /** A message legitimately has no subject, so an empty string is valid here. */
    private String subject(Object raw) {
        return raw instanceof String value ? value : "";
    }

    private static String render(List<MailMessage> messages) {
        return "{\"" + VERSION_FIELD + "\":" + FORMAT_VERSION
                + ",\"" + MESSAGES_FIELD + "\":["
                + String.join(",", messages.stream().map(MessageSnapshotJsonStore::entry).toList())
                + "]}\n";
    }

    private static String entry(MailMessage message) {
        return "{" + property(ID_FIELD, message.id().value())
                + "," + property(SENDER_FIELD, message.sender().value())
                + "," + property(SUBJECT_FIELD, message.subject())
                + "," + property(RECEIVED_AT_FIELD, message.receivedAt().toString())
                + ",\"" + LABELS_FIELD + "\":["
                + String.join(",", message.labels().stream()
                        .map(MessageSnapshotJsonStore::quoted).toList())
                + "]}";
    }

    private static String property(String name, String value) {
        return quoted(name) + ":" + quoted(value);
    }

    private static String quoted(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        value.codePoints().forEach(codePoint -> escaped.append(escape(codePoint)));
        return escaped.append('"').toString();
    }

    private static String escape(int codePoint) {
        return switch (codePoint) {
            case '"' -> "\\\"";
            case '\\' -> "\\\\";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            default -> codePoint < 0x20
                    ? "\\u%04x".formatted(codePoint)
                    : new String(Character.toChars(codePoint));
        };
    }
}
