package dev.inboxpilot.infrastructure.report;

import dev.inboxpilot.application.port.InventorySnapshotStore;
import dev.inboxpilot.application.port.InventorySnapshotStoreException;
import dev.inboxpilot.domain.inventory.DomainInventory;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.inventory.SenderInventory;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.infrastructure.config.InboxPilotProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Loads the deterministic inventory JSON report for downstream local workflows. */
@Component
public final class InventoryJsonSnapshotStore implements InventorySnapshotStore {

    private static final String INVENTORY_FILE = "inventory.json";
    private static final String READ_FAILURE =
            "Inventory report could not be read; run inventory first: ";

    private final Path path;
    private final Yaml yaml;

    @Autowired
    public InventoryJsonSnapshotStore(InboxPilotProperties properties) {
        this(properties.reports().outputDirectory().resolve(INVENTORY_FILE));
    }

    InventoryJsonSnapshotStore(Path path) {
        this.path = path;
        this.yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
    }

    @Override
    public Inventory load() {
        try {
            InventoryDocument document = document(yaml.load(Files.readString(path)));
            return new Inventory(
                    document.senders().stream().map(InventoryJsonSnapshotStore::sender).toList(),
                    document.domains().stream().map(InventoryJsonSnapshotStore::domain).toList());
        } catch (IOException | RuntimeException exception) {
            throw new InventorySnapshotStoreException(READ_FAILURE + path, exception);
        }
    }

    private static InventoryDocument document(Object raw) {
        java.util.Map<?, ?> root = map(raw);
        return new InventoryDocument(entries(root.get("senders")), entries(root.get("domains")));
    }

    private static List<EntryDocument> entries(Object raw) {
        if (!(raw instanceof List<?> values)) {
            throw new IllegalArgumentException(READ_FAILURE);
        }
        return values.stream().map(InventoryJsonSnapshotStore::entry).toList();
    }

    private static EntryDocument entry(Object raw) {
        java.util.Map<?, ?> value = map(raw);
        return new EntryDocument(
                optionalText(value.get("sender")), optionalText(value.get("domain")),
                integer(value.get("messageCount")), integer(value.get("unreadCount")),
                text(value.get("firstReceivedAt")), text(value.get("lastReceivedAt")),
                strings(value.get("currentLabels")), strings(value.get("sampleSubjects")));
    }

    private static java.util.Map<?, ?> map(Object raw) {
        if (raw instanceof java.util.Map<?, ?> value) {
            return value;
        }
        throw new IllegalArgumentException(READ_FAILURE);
    }

    private static int integer(Object raw) {
        if (raw instanceof Number value) {
            return value.intValue();
        }
        throw new IllegalArgumentException(READ_FAILURE);
    }

    private static String text(Object raw) {
        if (raw instanceof String value && !value.isBlank()) {
            return value;
        }
        throw new IllegalArgumentException(READ_FAILURE);
    }

    private static String optionalText(Object raw) {
        return raw == null ? null : text(raw);
    }

    private static List<String> strings(Object raw) {
        if (!(raw instanceof List<?> values)) {
            throw new IllegalArgumentException(READ_FAILURE);
        }
        return values.stream().map(InventoryJsonSnapshotStore::text).toList();
    }

    private static SenderInventory sender(EntryDocument entry) {
        return new SenderInventory(new EmailAddress(entry.sender()), statistics(entry));
    }

    private static DomainInventory domain(EntryDocument entry) {
        return new DomainInventory(entry.domain(), statistics(entry));
    }

    private static InventoryStatistics statistics(EntryDocument entry) {
        return new InventoryStatistics(
                entry.messageCount(), entry.unreadCount(),
                Instant.parse(entry.firstReceivedAt()), Instant.parse(entry.lastReceivedAt()),
                entry.currentLabels(), entry.sampleSubjects());
    }

    private record InventoryDocument(
            List<EntryDocument> senders, List<EntryDocument> domains) {
    }

    private record EntryDocument(
            String sender,
            String domain,
            int messageCount,
            int unreadCount,
            String firstReceivedAt,
            String lastReceivedAt,
            List<String> currentLabels,
            List<String> sampleSubjects) {
    }
}
