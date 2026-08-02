package dev.inboxpilot.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.analysis.AnalysisCleanupCandidate;
import dev.inboxpilot.application.analysis.AnalysisLabelConflict;
import dev.inboxpilot.application.analysis.AnalysisReport;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.inventory.SenderInventory;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.rules.RuleSet;
import dev.inboxpilot.infrastructure.config.ReportFormat;
import dev.inboxpilot.infrastructure.config.ReportProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnalysisReportWriterTest {

    private static final String SENDER = "news@example.com";
    private static final String LABEL_ONE = "Label_news";
    private static final String LABEL_TWO = "Label_read";
    private static final String REASON = "high-volume-unlabeled-sender";
    private static final Instant RECEIVED_AT = Instant.parse("2026-08-02T12:00:00Z");
    private static final int MESSAGE_COUNT = 25;
    private static final int UNREAD_COUNT = 4;

    @TempDir
    Path directory;

    @Test
    void writesTheCompleteDeterministicArtifactSet() throws Exception {
        ReportProperties properties = new ReportProperties(
                directory, List.of(ReportFormat.JSON), false);
        AnalysisReportWriter writer = new AnalysisReportWriter(properties);

        List<Path> paths = writer.write(report());

        assertThat(paths).extracting(path -> path.getFileName().toString())
                .containsExactly("summary.json", "unlabeled-senders.csv",
                        "label-conflicts.csv", "cleanup-candidates.csv",
                        "rule-suggestions.yaml");
        assertThat(Files.readString(directory.resolve("summary.json")))
                .contains("\"processedMessages\":25");
        assertThat(Files.readString(directory.resolve("unlabeled-senders.csv")))
                .contains(SENDER);
        assertThat(Files.readString(directory.resolve("label-conflicts.csv")))
                .contains(LABEL_ONE + ";" + LABEL_TWO);
        assertThat(Files.readString(directory.resolve("cleanup-candidates.csv")))
                .contains(REASON);
        assertThat(Files.readString(directory.resolve("rule-suggestions.yaml")))
                .startsWith("version: 1\nrules:");
    }

    private static AnalysisReport report() {
        EmailAddress sender = new EmailAddress(SENDER);
        SenderInventory unlabeled = new SenderInventory(sender, new InventoryStatistics(
                MESSAGE_COUNT, UNREAD_COUNT, RECEIVED_AT, RECEIVED_AT,
                List.of(), List.of("Newsletter")));
        return new AnalysisReport(
                MESSAGE_COUNT,
                List.of(unlabeled),
                List.of(new AnalysisLabelConflict(
                        sender, List.of(LABEL_ONE, LABEL_TWO), MESSAGE_COUNT)),
                List.of(new AnalysisCleanupCandidate(
                        sender, MESSAGE_COUNT, UNREAD_COUNT, REASON)),
                new RuleSet(RuleSet.CURRENT_VERSION, List.of()));
    }
}
