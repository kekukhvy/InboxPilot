package dev.inboxpilot.infrastructure.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.model.DashboardDocument;
import dev.inboxpilot.application.model.DashboardDocumentSummary;
import dev.inboxpilot.infrastructure.config.DashboardProperties;
import dev.inboxpilot.infrastructure.config.ReportFormat;
import dev.inboxpilot.infrastructure.config.ReportProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileDashboardDocumentStoreTest {

    private static final long PREVIEW_LIMIT = 1_024;

    @TempDir
    Path temporaryDirectory;

    @Test
    void listsOnlySupportedReportsAndTheConfiguredRuleFile() throws Exception {
        Path reports = Files.createDirectory(temporaryDirectory.resolve("reports"));
        Files.writeString(reports.resolve("inventory.json"), "{}");
        Files.writeString(reports.resolve("notes.txt"), "private note");
        Path rules = temporaryDirectory.resolve("rules.yaml");
        Files.writeString(rules, "version: 1");

        FileDashboardDocumentStore store = store(reports, rules, PREVIEW_LIMIT);

        assertThat(store.list()).extracting(DashboardDocumentSummary::name)
                .containsExactly("inventory.json", "rules.yaml");
    }

    @Test
    void readsAListedDocumentWithoutExposingItsFilesystemPath() throws Exception {
        Path reports = Files.createDirectory(temporaryDirectory.resolve("reports"));
        Files.writeString(reports.resolve("inventory.csv"), "sender,count\nexample.com,2");
        FileDashboardDocumentStore store = store(
                reports, temporaryDirectory.resolve("missing-rules.yaml"), PREVIEW_LIMIT);
        DashboardDocumentSummary summary = store.list().getFirst();

        DashboardDocument document = store.findById(summary.id()).orElseThrow();

        assertThat(document.content()).contains("example.com,2");
        assertThat(document.mediaType()).isEqualTo("text/csv");
        assertThat(document.name()).isEqualTo("inventory.csv");
    }

    @Test
    void refusesUnknownAndOversizedDocuments() throws Exception {
        Path reports = Files.createDirectory(temporaryDirectory.resolve("reports"));
        Files.writeString(reports.resolve("large.json"), "0123456789");
        FileDashboardDocumentStore store = store(
                reports, temporaryDirectory.resolve("rules.yaml"), 4);

        assertThat(store.list()).isEmpty();
        assertThat(store.findById("not-a-valid-id")).isEmpty();
    }

    private static FileDashboardDocumentStore store(Path reports, Path rules, long limit) {
        ReportProperties reportProperties =
                new ReportProperties(reports, List.of(ReportFormat.JSON), false);
        DashboardProperties dashboardProperties = new DashboardProperties(true, rules, limit);
        return new FileDashboardDocumentStore(reportProperties, dashboardProperties);
    }
}
