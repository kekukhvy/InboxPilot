package dev.inboxpilot.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.infrastructure.config.ReportFormat;
import dev.inboxpilot.infrastructure.config.ReportProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("InventoryReportWriter")
class InventoryReportWriterTest {

    private static final String CSV_FILE = "inventory.csv";
    private static final String JSON_FILE = "inventory.json";
    private static final String HTML_FILE = "inventory.html";

    @TempDir
    Path directory;

    @Test
    @DisplayName("writes configured CSV, JSON, and HTML reports")
    void writesConfiguredReports() {
        ReportProperties properties = new ReportProperties(
                directory, List.of(ReportFormat.CSV, ReportFormat.JSON, ReportFormat.HTML), false);
        InventoryReportWriter writer = new InventoryReportWriter(properties);

        List<Path> paths = writer.write(new Inventory(List.of(), List.of()));

        assertThat(paths).containsExactly(
                directory.resolve(CSV_FILE), directory.resolve(JSON_FILE), directory.resolve(HTML_FILE));
        assertThat(paths).allMatch(Files::exists);
    }
}
