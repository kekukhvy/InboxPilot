package dev.inboxpilot.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.classification.ClassificationDryRunEntry;
import dev.inboxpilot.application.classification.ClassificationDryRunReport;
import dev.inboxpilot.application.classification.ClassificationDryRunSummary;
import dev.inboxpilot.domain.classification.MessageLabelPlan;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.domain.rules.RuleId;
import dev.inboxpilot.infrastructure.config.ReportFormat;
import dev.inboxpilot.infrastructure.config.ReportProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClassificationDryRunReportWriterTest {

    @TempDir
    Path directory;

    @Test
    void writesAllFourDeterministicReports() throws Exception {
        ClassificationDryRunReportWriter writer = new ClassificationDryRunReportWriter(
                new ReportProperties(directory, List.of(ReportFormat.JSON), false));
        MailMessage message = new MailMessage(new MessageId("message-1"),
                new EmailAddress("news@example.com"), "Digest",
                Instant.parse("2026-08-01T00:00:00Z"), List.of("Old"));
        MessageLabelPlan plan = new MessageLabelPlan(message.id(), List.of(new RuleId("rule-1")),
                List.of("Old"), List.of("New"), List.of(), List.of("New", "Old"));
        ClassificationDryRunEntry entry = new ClassificationDryRunEntry(message, plan, "");
        ClassificationDryRunReport report = new ClassificationDryRunReport(
                new ClassificationDryRunSummary(1, 1, 0, 0, 1, 1, 0, 0, 0),
                List.of(entry), List.of(), List.of());

        List<Path> paths = writer.write(report);

        assertThat(paths).extracting(path -> path.getFileName().toString()).containsExactly(
                "classification-dry-run-summary.json",
                "classification-dry-run-changes.csv",
                "classification-dry-run-conflicts.csv",
                "classification-dry-run-unmatched.csv");
        assertThat(Files.readString(paths.getFirst()))
                .contains("\"processedMessages\":1", "\"proposedLabelAdditions\":1");
        assertThat(Files.readString(paths.get(1)))
                .contains("messageId,sender,subject,currentLabels,labelsToAdd,labelsToRemove,matchedRuleIds")
                .contains("rule-1");
    }
}
