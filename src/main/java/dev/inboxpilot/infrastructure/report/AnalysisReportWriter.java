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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Writes the deterministic local-analysis artifact set. */
@Component
public final class AnalysisReportWriter implements AnalysisReportStore {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisReportWriter.class);

    private static final String SUMMARY_FILE = "summary.json";
    private static final String UNLABELED_FILE = "unlabeled-senders.csv";
    private static final String CONFLICTS_FILE = "label-conflicts.csv";
    private static final String CLEANUP_FILE = "cleanup-candidates.csv";
    private static final String SUGGESTIONS_FILE = "rule-suggestions.yaml";
    private static final String REJECTED_FILE = "rejected-rule-suggestions.csv";
    private static final String VALIDATION_FILE = "rule-validation-errors.csv";
    private static final String WRITE_FAILURE = "Analysis reports could not be written: ";
    private static final String STAGING_PREFIX = ".analysis-staging-";
    private static final String BACKUP_PREFIX = ".analysis-backup-";
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
        Path stagingDirectory = null;
        Path backupDirectory = null;
        try {
            Files.createDirectories(properties.outputDirectory());
            Map<String, String> artifacts = artifacts(report);
            stagingDirectory = stage(artifacts);
            backupDirectory = Files.createTempDirectory(
                    properties.outputDirectory(), BACKUP_PREFIX);
            replaceAnalysisArtifacts(stagingDirectory, backupDirectory, artifacts.keySet());
            return artifacts.keySet().stream()
                    .map(properties.outputDirectory()::resolve).toList();
        } catch (IOException exception) {
            throw new ReportWriteException(
                    WRITE_FAILURE + properties.outputDirectory(), exception);
        } finally {
            deleteDirectory(stagingDirectory);
            deleteDirectory(backupDirectory);
        }
    }

    private Map<String, String> artifacts(AnalysisReport report) {
        Map<String, String> artifacts = new LinkedHashMap<>();
        artifacts.put(SUMMARY_FILE, summary(report));
        artifacts.put(UNLABELED_FILE, unlabeled(report));
        artifacts.put(CONFLICTS_FILE, conflicts(report));
        artifacts.put(CLEANUP_FILE, cleanup(report));
        artifacts.put(REJECTED_FILE, rejected(report));
        artifacts.put(VALIDATION_FILE, validation(report));
        if (report.ruleValidationFindings().isEmpty()) {
            artifacts.put(SUGGESTIONS_FILE, yamlRenderer.render(report.ruleGeneration().rules()));
        }
        return artifacts;
    }

    private Path stage(Map<String, String> artifacts) throws IOException {
        Path directory = Files.createTempDirectory(
                properties.outputDirectory(), STAGING_PREFIX);
        for (Map.Entry<String, String> artifact : artifacts.entrySet()) {
            Files.writeString(directory.resolve(artifact.getKey()), artifact.getValue(),
                    StandardCharsets.UTF_8);
        }
        return directory;
    }

    private void replaceAnalysisArtifacts(
            Path stagingDirectory,
            Path backupDirectory,
            java.util.Set<String> generatedFiles) throws IOException {
        List<String> analysisFiles = List.of(
                SUMMARY_FILE, UNLABELED_FILE, CONFLICTS_FILE, CLEANUP_FILE,
                REJECTED_FILE, VALIDATION_FILE, SUGGESTIONS_FILE);
        List<String> backedUpFiles = new java.util.ArrayList<>();
        List<String> installedFiles = new java.util.ArrayList<>();
        try {
            backupExisting(analysisFiles, backupDirectory, backedUpFiles);
            installGenerated(generatedFiles, stagingDirectory, installedFiles);
        } catch (IOException exception) {
            try {
                restoreBackup(backedUpFiles, installedFiles, backupDirectory);
            } catch (IOException rollbackFailure) {
                exception.addSuppressed(rollbackFailure);
            }
            throw exception;
        }
    }

    private void backupExisting(
            List<String> files, Path backupDirectory, List<String> backedUpFiles)
            throws IOException {
        for (String file : files) {
            Path existing = properties.outputDirectory().resolve(file);
            if (Files.exists(existing)) {
                move(existing, backupDirectory.resolve(file));
                backedUpFiles.add(file);
            }
        }
    }

    private void installGenerated(
            java.util.Set<String> files, Path stagingDirectory, List<String> installedFiles)
            throws IOException {
        for (String file : files) {
            move(stagingDirectory.resolve(file), properties.outputDirectory().resolve(file));
            installedFiles.add(file);
        }
    }

    private void restoreBackup(
            List<String> backedUpFiles,
            List<String> installedFiles,
            Path backupDirectory) throws IOException {
        IOException failure = null;
        for (String file : installedFiles) {
            try {
                Files.deleteIfExists(properties.outputDirectory().resolve(file));
            } catch (IOException exception) {
                failure = combine(failure, exception);
            }
        }
        for (String file : backedUpFiles) {
            try {
                move(backupDirectory.resolve(file), properties.outputDirectory().resolve(file));
            } catch (IOException exception) {
                failure = combine(failure, exception);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static IOException combine(IOException failure, IOException next) {
        if (failure == null) {
            return next;
        }
        failure.addSuppressed(next);
        return failure;
    }

    private static void move(Path source, Path target) throws IOException {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void deleteDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (var entries = Files.list(directory)) {
            entries.forEach(AnalysisReportWriter::deleteQuietly);
            Files.deleteIfExists(directory);
        } catch (IOException exception) {
            logger.warn("Temporary analysis report directory could not be removed: {}",
                    directory, exception);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            logger.warn("Temporary analysis report artifact could not be removed: {}",
                    path, exception);
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
