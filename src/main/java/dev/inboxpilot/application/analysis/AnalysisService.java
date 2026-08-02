package dev.inboxpilot.application.analysis;

import dev.inboxpilot.application.port.InventorySnapshotStore;
import dev.inboxpilot.application.port.AnalysisReportStore;
import dev.inboxpilot.application.port.MailboxGateway;
import dev.inboxpilot.application.model.MailboxLabelCatalog;
import dev.inboxpilot.domain.analysis.UnclassifiedInventory;
import dev.inboxpilot.domain.analysis.UnclassifiedInventoryAnalyzer;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.inventory.SenderInventory;
import dev.inboxpilot.domain.rules.RuleGenerationResult;
import dev.inboxpilot.domain.rules.RuleGenerationPolicy;
import dev.inboxpilot.domain.rules.StarterRuleGenerator;
import dev.inboxpilot.domain.rules.GeneratedRuleValidator;
import dev.inboxpilot.domain.rules.RuleValidationSeverity;
import java.util.Objects;
import java.util.Set;

/** Orchestrates deterministic, read-only mailbox analysis. */
public final class AnalysisService {

    private static final String USER_LABEL_PREFIX = "Label_";
    private static final String CLEANUP_REASON = "high-volume-unlabeled-sender";
    private static final int MINIMUM_REVIEW_MESSAGE_COUNT = 20;
    private static final int MINIMUM_CONFLICTING_LABELS = 2;

    private final InventorySnapshotStore inventoryStore;
    private final AnalysisReportStore reportStore;
    private final MailboxGateway mailboxGateway;
    private final RuleGenerationPolicy ruleGenerationPolicy;
    private final UnclassifiedInventoryAnalyzer unclassifiedAnalyzer =
            new UnclassifiedInventoryAnalyzer();

    private final StarterRuleGenerator ruleGenerator = new StarterRuleGenerator();
    private final GeneratedRuleValidator ruleValidator = new GeneratedRuleValidator();

    public AnalysisService(
            InventorySnapshotStore inventoryStore,
            AnalysisReportStore reportStore,
            MailboxGateway mailboxGateway,
            RuleGenerationPolicy ruleGenerationPolicy) {
        this.inventoryStore = Objects.requireNonNull(inventoryStore, "inventoryStore");
        this.reportStore = Objects.requireNonNull(reportStore, "reportStore");
        this.mailboxGateway = Objects.requireNonNull(mailboxGateway, "mailboxGateway");
        this.ruleGenerationPolicy = Objects.requireNonNull(
                ruleGenerationPolicy, "ruleGenerationPolicy");
    }

    public AnalysisRunResult run() {
        Inventory inventory = inventoryStore.load();
        MailboxLabelCatalog labels = new MailboxLabelCatalog(mailboxGateway.listLabels());
        Set<String> userLabelIds = labels.userLabelNamesById().keySet();
        UnclassifiedInventory unclassified = unclassifiedAnalyzer.analyze(
                inventory, userLabelIds);
        int processedMessages = inventory.senders().stream()
                .map(sender -> sender.statistics())
                .mapToInt(InventoryStatistics::messageCount)
                .sum();
        AnalysisReport report = report(
                inventory, labels, unclassified, processedMessages);
        java.util.List<java.nio.file.Path> paths = reportStore.write(report);
        if (report.ruleValidationFindings().stream()
                .anyMatch(finding -> finding.severity() == RuleValidationSeverity.ERROR)) {
            throw new RuleValidationFailureException(
                    "Generated rules failed validation; see rule-validation-errors.csv");
        }
        return new AnalysisRunResult(processedMessages, unclassified.senders().size(),
                unclassified.domains().size(), paths);
    }

    private static boolean isUserLabel(String labelId) {
        return labelId.startsWith(USER_LABEL_PREFIX);
    }

    private AnalysisReport report(
            Inventory inventory,
            MailboxLabelCatalog labels,
            UnclassifiedInventory unclassified,
            int processedMessages) {
        RuleGenerationResult suggestions = ruleGenerator.generate(
                inventory, labels.userLabelNamesById(), ruleGenerationPolicy);
        var validation = ruleValidator.validate(
                suggestions.rules().rules(), ruleGenerationPolicy);
        return new AnalysisReport(
                processedMessages,
                displaySenders(unclassified.senders(), labels),
                conflicts(inventory, labels),
                cleanupCandidates(unclassified.senders()),
                suggestions,
                validation);
    }

    private static java.util.List<AnalysisLabelConflict> conflicts(
            Inventory inventory, MailboxLabelCatalog labels) {
        return inventory.senders().stream()
                .map(sender -> conflict(sender, labels))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private static java.util.Optional<AnalysisLabelConflict> conflict(
            SenderInventory sender, MailboxLabelCatalog catalog) {
        java.util.List<String> labels = sender.statistics().currentLabels().stream()
                .filter(AnalysisService::isUserLabel)
                .map(catalog::nameForId)
                .sorted()
                .toList();
        if (labels.size() < MINIMUM_CONFLICTING_LABELS) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new AnalysisLabelConflict(
                sender.sender(), labels, sender.statistics().messageCount()));
    }

    private static java.util.List<SenderInventory> displaySenders(
            java.util.List<SenderInventory> senders, MailboxLabelCatalog labels) {
        return senders.stream()
                .map(sender -> new SenderInventory(
                        sender.sender(), displayStatistics(sender.statistics(), labels)))
                .toList();
    }

    private static InventoryStatistics displayStatistics(
            InventoryStatistics statistics, MailboxLabelCatalog labels) {
        java.util.List<String> names = statistics.currentLabels().stream()
                .map(labels::nameForId)
                .sorted()
                .toList();
        return new InventoryStatistics(
                statistics.messageCount(), statistics.unreadCount(),
                statistics.firstReceivedAt(), statistics.lastReceivedAt(),
                names, statistics.sampleSubjects());
    }

    private static java.util.List<AnalysisCleanupCandidate> cleanupCandidates(
            java.util.List<SenderInventory> senders) {
        return senders.stream()
                .filter(sender -> sender.statistics().messageCount()
                        >= MINIMUM_REVIEW_MESSAGE_COUNT)
                .map(sender -> new AnalysisCleanupCandidate(
                        sender.sender(), sender.statistics().messageCount(),
                        sender.statistics().unreadCount(), CLEANUP_REASON))
                .toList();
    }
}
