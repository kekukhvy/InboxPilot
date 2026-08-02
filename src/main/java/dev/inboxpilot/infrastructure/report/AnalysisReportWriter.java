package dev.inboxpilot.infrastructure.report;

import dev.inboxpilot.application.analysis.AnalysisCleanupCandidate;
import dev.inboxpilot.application.analysis.AnalysisLabelConflict;
import dev.inboxpilot.application.analysis.AnalysisReport;
import dev.inboxpilot.application.port.AnalysisReportStore;
import dev.inboxpilot.domain.inventory.SenderInventory;
import dev.inboxpilot.infrastructure.config.InboxPilotProperties;
import dev.inboxpilot.infrastructure.config.ReportProperties;
import dev.inboxpilot.infrastructure.rules.RuleYamlRenderer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Writes the deterministic local-analysis artifact set. */
@Component
public final class AnalysisReportWriter implements AnalysisReportStore {

    private static final String SUMMARY_FILE = "summary.json";
    private static final String UNLABELED_FILE = "unlabeled-senders.csv";
    private static final String CONFLICTS_FILE = "label-conflicts.csv";
    private static final String CLEANUP_FILE = "cleanup-candidates.csv";
    private static final String SUGGESTIONS_FILE = "rule-suggestions.yaml";
    private static final String REJECTED_FILE = "rejected-rule-suggestions.csv";
    private static final String VALIDATION_FILE = "rule-validation-errors.csv";
    private static final String WRITE_FAILURE = "Analysis reports could not be written: ";
    private static final String LINE_SEPARATOR = "\n";
    private static final String LIST_SEPARATOR = ";";
    private static final String SUMMARY_TEMPLATE =
            "{\"processedMessages\":%d,\"unlabeledSenders\":%d,"
                    + "\"labelConflicts\":%d,\"cleanupCandidates\":%d,"
                    + "\"generatedRuleSuggestions\":%d,"
                    + "\"deduplicatedRuleSuggestions\":%d,"
                    + "\"rejectedRuleSuggestions\":%d,"
                    + "\"finalRuleSuggestions\":%d}\n";
    private static final String UNLABELED_HEADER =
            "sender,domain,messageCount,unreadCount,currentLabels,sampleSubjects\n";
    private static final String CONFLICTS_HEADER = "sender,messageCount,labels\n";
    private static final String CLEANUP_HEADER = "sender,messageCount,unreadCount,reason\n";
    private static final String REJECTED_HEADER = "sender,domain,messageCount,reason\n";
    private static final String VALIDATION_HEADER = "ruleId,severity,errorCode,message\n";

    private final ReportProperties properties;
    private final RuleYamlRenderer yamlRenderer = new RuleYamlRenderer();

    @Autowired
    public AnalysisReportWriter(InboxPilotProperties properties) {
        this(properties.reports());
    }

    AnalysisReportWriter(ReportProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<Path> write(AnalysisReport report) {
        try {
            Files.createDirectories(properties.outputDirectory());
            List<Path> paths = new java.util.ArrayList<>(List.of(
                    write(SUMMARY_FILE, summary(report)),
                    write(UNLABELED_FILE, unlabeled(report)),
                    write(CONFLICTS_FILE, conflicts(report)),
                    write(CLEANUP_FILE, cleanup(report)),
                    write(REJECTED_FILE, rejected(report)),
                    write(VALIDATION_FILE, validation(report))));
            if (report.ruleValidationFindings().isEmpty()) {
                paths.add(write(SUGGESTIONS_FILE,
                        yamlRenderer.render(report.ruleGeneration().rules())));
            } else if (properties.overwrite()) {
                Files.deleteIfExists(properties.outputDirectory().resolve(SUGGESTIONS_FILE));
            }
            return List.copyOf(paths);
        } catch (IOException exception) {
            throw new ReportWriteException(
                    WRITE_FAILURE + properties.outputDirectory(), exception);
        }
    }

    private static String summary(AnalysisReport report) {
        return SUMMARY_TEMPLATE.formatted(
                report.processedMessages(), report.unlabeledSenders().size(),
                report.labelConflicts().size(), report.cleanupCandidates().size(),
                report.ruleGeneration().generatedSuggestions(),
                report.ruleGeneration().deduplicatedSuggestions(),
                report.ruleGeneration().rejectedSuggestions().size(),
                report.ruleGeneration().finalSuggestions());
    }

    private static String unlabeled(AnalysisReport report) {
        return UNLABELED_HEADER + String.join(LINE_SEPARATOR,
                report.unlabeledSenders().stream()
                        .map(AnalysisReportWriter::unlabeledRow).toList())
                + trailingLine(report.unlabeledSenders());
    }

    private static String unlabeledRow(SenderInventory sender) {
        var statistics = sender.statistics();
        return row(sender.sender().value(), sender.sender().domain(),
                Integer.toString(statistics.messageCount()),
                Integer.toString(statistics.unreadCount()),
                String.join(LIST_SEPARATOR, statistics.currentLabels()),
                String.join(LIST_SEPARATOR, statistics.sampleSubjects()));
    }

    private static String conflicts(AnalysisReport report) {
        return CONFLICTS_HEADER + String.join(LINE_SEPARATOR,
                report.labelConflicts().stream()
                        .map(AnalysisReportWriter::conflictRow).toList())
                + trailingLine(report.labelConflicts());
    }

    private static String conflictRow(AnalysisLabelConflict conflict) {
        return row(conflict.sender().value(), Integer.toString(conflict.messageCount()),
                String.join(LIST_SEPARATOR, conflict.labels()));
    }

    private static String cleanup(AnalysisReport report) {
        return CLEANUP_HEADER + String.join(LINE_SEPARATOR,
                report.cleanupCandidates().stream()
                        .map(AnalysisReportWriter::cleanupRow).toList())
                + trailingLine(report.cleanupCandidates());
    }

    private static String cleanupRow(AnalysisCleanupCandidate candidate) {
        return row(candidate.sender().value(), Integer.toString(candidate.messageCount()),
                Integer.toString(candidate.unreadCount()), candidate.reason());
    }

    private static String rejected(AnalysisReport report) {
        var rejected = report.ruleGeneration().rejectedSuggestions();
        return REJECTED_HEADER + String.join(LINE_SEPARATOR, rejected.stream()
                .map(value -> row(value.sender(), value.domain(),
                        Integer.toString(value.messageCount()), value.reason()))
                .toList()) + trailingLine(rejected);
    }

    private static String validation(AnalysisReport report) {
        var findings = report.ruleValidationFindings();
        return VALIDATION_HEADER + String.join(LINE_SEPARATOR, findings.stream()
                .map(value -> row(value.ruleId(), value.severity().name(),
                        value.errorCode(), value.message()))
                .toList()) + trailingLine(findings);
    }

    private Path write(String fileName, String content) throws IOException {
        Path path = properties.outputDirectory().resolve(fileName);
        Files.writeString(path, content, StandardCharsets.UTF_8, openOptions());
        return path;
    }

    private OpenOption[] openOptions() {
        if (properties.overwrite()) {
            return new OpenOption[] {
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};
        }
        return new OpenOption[] {StandardOpenOption.CREATE_NEW};
    }

    private static String trailingLine(List<?> values) {
        return values.isEmpty() ? "" : LINE_SEPARATOR;
    }

    private static String row(String... values) {
        return String.join(",", java.util.Arrays.stream(values)
                .map(AnalysisReportWriter::csv).toList());
    }

    private static String csv(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
