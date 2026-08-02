package dev.inboxpilot.infrastructure.report;

import dev.inboxpilot.application.classification.ClassificationDryRunEntry;
import dev.inboxpilot.application.classification.ClassificationDryRunReport;
import dev.inboxpilot.application.classification.ClassificationDryRunSummary;
import dev.inboxpilot.application.port.ClassificationDryRunReportStore;
import dev.inboxpilot.infrastructure.config.InboxPilotProperties;
import dev.inboxpilot.infrastructure.config.ReportProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Writes deterministic classification dry-run review artifacts. */
@Component
public final class ClassificationDryRunReportWriter
        implements ClassificationDryRunReportStore {

    private static final String SUMMARY_FILE = "classification-dry-run-summary.json";
    private static final String CHANGES_FILE = "classification-dry-run-changes.csv";
    private static final String CONFLICTS_FILE = "classification-dry-run-conflicts.csv";
    private static final String UNMATCHED_FILE = "classification-dry-run-unmatched.csv";
    private static final String WRITE_FAILURE = "Classification dry-run reports could not be written: ";
    private static final String CHANGES_HEADER =
            "messageId,sender,subject,currentLabels,labelsToAdd,labelsToRemove,matchedRuleIds\n";
    private static final String CONFLICTS_HEADER =
            "messageId,sender,subject,matchedRuleIds,proposedLabels,reason\n";
    private static final String UNMATCHED_HEADER =
            "messageId,sender,subject,currentLabels\n";
    private static final String SEPARATOR = ";";
    private static final String LINE_SEPARATOR = "\n";
    private static final String SUMMARY_TEMPLATE = """
            {"processedMessages":%d,"matchedMessages":%d,"unmatchedMessages":%d,"alreadyCorrectlyLabeled":%d,"messagesWithProposedChanges":%d,"proposedLabelAdditions":%d,"proposedLabelRemovals":%d,"messagesMatchedByMultipleRules":%d,"ruleConflicts":%d}
            """;

    private final ReportProperties properties;

    @Autowired
    public ClassificationDryRunReportWriter(InboxPilotProperties properties) {
        this(properties.reports());
    }

    ClassificationDryRunReportWriter(ReportProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<Path> write(ClassificationDryRunReport report) {
        try {
            Files.createDirectories(properties.outputDirectory());
            return List.of(
                    write(SUMMARY_FILE, summary(report.summary())),
                    write(CHANGES_FILE, csv(CHANGES_HEADER, report.changes(), this::change)),
                    write(CONFLICTS_FILE, csv(CONFLICTS_HEADER, report.conflicts(), this::conflict)),
                    write(UNMATCHED_FILE, csv(UNMATCHED_HEADER, report.unmatched(), this::unmatched)));
        } catch (IOException exception) {
            throw new ReportWriteException(WRITE_FAILURE + properties.outputDirectory(), exception);
        }
    }

    private static String summary(ClassificationDryRunSummary summary) {
        return SUMMARY_TEMPLATE.formatted(
                summary.processedMessages(), summary.matchedMessages(),
                summary.unmatchedMessages(), summary.alreadyCorrectlyLabeled(),
                summary.messagesWithProposedChanges(), summary.proposedLabelAdditions(),
                summary.proposedLabelRemovals(), summary.messagesMatchedByMultipleRules(),
                summary.ruleConflicts());
    }

    private String change(ClassificationDryRunEntry entry) {
        return row(entry.message().id().value(), entry.message().sender().value(),
                entry.message().subject(), join(entry.plan().oldLabels()),
                join(entry.plan().addLabels()), join(entry.plan().removeLabels()),
                join(entry.plan().matchedRuleIds().stream().map(id -> id.value()).toList()));
    }

    private String conflict(ClassificationDryRunEntry entry) {
        return row(entry.message().id().value(), entry.message().sender().value(),
                entry.message().subject(),
                join(entry.plan().matchedRuleIds().stream().map(id -> id.value()).toList()),
                join(entry.plan().newLabels()), entry.reason());
    }

    private String unmatched(ClassificationDryRunEntry entry) {
        return row(entry.message().id().value(), entry.message().sender().value(),
                entry.message().subject(), join(entry.plan().oldLabels()));
    }

    private static <T> String csv(
            String header, List<T> entries, java.util.function.Function<T, String> renderer) {
        String body = String.join(LINE_SEPARATOR, entries.stream().map(renderer).toList());
        return header + body + (entries.isEmpty() ? "" : LINE_SEPARATOR);
    }

    private Path write(String fileName, String content) throws IOException {
        Path path = properties.outputDirectory().resolve(fileName);
        Files.writeString(path, content, StandardCharsets.UTF_8, openOptions());
        return path;
    }

    private OpenOption[] openOptions() {
        return properties.overwrite()
                ? new OpenOption[] {StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING}
                : new OpenOption[] {StandardOpenOption.CREATE_NEW};
    }

    private static String join(List<String> values) {
        return String.join(SEPARATOR, values);
    }

    private static String row(String... values) {
        return String.join(",", Arrays.stream(values)
                .map(ClassificationDryRunReportWriter::quoted).toList());
    }

    private static String quoted(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
