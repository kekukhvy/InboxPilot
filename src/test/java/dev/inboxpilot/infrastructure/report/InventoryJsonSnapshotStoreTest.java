package dev.inboxpilot.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import dev.inboxpilot.application.port.InventorySnapshotStoreException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InventoryJsonSnapshotStoreTest {

    private static final String FILE_NAME = "inventory.json";
    private static final String SENDER = "sender@example.com";
    private static final String JSON = """
            {"senders":[{"sender":"sender@example.com","messageCount":2,"unreadCount":1,
            "firstReceivedAt":"2026-08-01T10:00:00Z","lastReceivedAt":"2026-08-02T10:00:00Z",
            "currentLabels":["Label_work"],"sampleSubjects":["Subject"]}],
            "domains":[{"domain":"example.com","messageCount":2,"unreadCount":1,
            "firstReceivedAt":"2026-08-01T10:00:00Z","lastReceivedAt":"2026-08-02T10:00:00Z",
            "currentLabels":["Label_work"],"sampleSubjects":["Subject"]}]}
            """;

    @TempDir
    Path directory;

    @Test
    void restoresThePersistedInventoryContract() throws IOException {
        Path path = directory.resolve(FILE_NAME);
        Files.writeString(path, JSON);

        var inventory = new InventoryJsonSnapshotStore(path).load();

        assertThat(inventory.senders()).singleElement().satisfies(sender -> {
            assertThat(sender.sender().value()).isEqualTo(SENDER);
            assertThat(sender.statistics().messageCount()).isEqualTo(2);
        });
        assertThat(inventory.domains()).hasSize(1);
    }

    @Test
    void explainsThatInventoryMustRunBeforeAnalysis() {
        Path missing = directory.resolve(FILE_NAME);

        assertThatExceptionOfType(InventorySnapshotStoreException.class)
                .isThrownBy(() -> new InventoryJsonSnapshotStore(missing).load())
                .withMessageContaining("run inventory first")
                .withMessageContaining(missing.toString());
    }
}
