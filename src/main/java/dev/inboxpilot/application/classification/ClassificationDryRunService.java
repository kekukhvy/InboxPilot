package dev.inboxpilot.application.classification;

import dev.inboxpilot.application.model.MailboxLabelCatalog;
import dev.inboxpilot.application.port.ClassificationDryRunReportStore;
import dev.inboxpilot.application.port.InventorySnapshotStore;
import dev.inboxpilot.application.port.MailboxGateway;
import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.application.port.RuleSetStore;
import dev.inboxpilot.domain.classification.ClassificationDryRun;
import dev.inboxpilot.domain.classification.ClassificationPlan;
import dev.inboxpilot.domain.classification.MessageLabelPlan;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.rules.RuleActionSpec;
import dev.inboxpilot.domain.rules.RuleDefinition;
import dev.inboxpilot.domain.rules.GeneratedRuleValidator;
import dev.inboxpilot.domain.rules.RuleGenerationPolicy;
import dev.inboxpilot.domain.rules.RuleId;
import dev.inboxpilot.domain.rules.RuleSet;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Runs classification against read-only metadata and exposes no mutation port. */
public final class ClassificationDryRunService {

    private static final String CONFLICT_REASON = "incompatible-label-actions";
    private static final String NO_REASON = "";

    private final InventorySnapshotStore inventoryStore;
    private final RuleSetStore ruleStore;
    private final MessageSource messageSource;
    private final MailboxGateway mailboxGateway;
    private final ClassificationDryRunReportStore reportStore;
    private final RuleGenerationPolicy rulePolicy;
    private final ClassificationDryRun planner = new ClassificationDryRun();
    private final GeneratedRuleValidator validator = new GeneratedRuleValidator();

    public ClassificationDryRunService(
            InventorySnapshotStore inventoryStore,
            RuleSetStore ruleStore,
            MessageSource messageSource,
            MailboxGateway mailboxGateway,
            ClassificationDryRunReportStore reportStore,
            RuleGenerationPolicy rulePolicy) {
        this.inventoryStore = inventoryStore;
        this.ruleStore = ruleStore;
        this.messageSource = messageSource;
        this.mailboxGateway = mailboxGateway;
        this.reportStore = reportStore;
        this.rulePolicy = rulePolicy;
    }

    public ClassificationDryRunResult run(Path rulesPath) {
        Inventory inventory = inventoryStore.load();
        RuleSet rules = ruleStore.load(rulesPath);
        validate(rules);
        MailboxLabelCatalog labels = new MailboxLabelCatalog(mailboxGateway.listLabels());
        List<MailMessage> messages = displayMessages(fetchMessages(inventory), labels);
        ClassificationPlan plan = planner.plan(rules, messages);
        ClassificationDryRunReport report = report(rules, messages, plan);
        return new ClassificationDryRunResult(report.summary(), reportStore.write(report));
    }

    private void validate(RuleSet rules) {
        var findings = validator.validate(rules.rules(), rulePolicy);
        if (!findings.isEmpty()) {
            throw new ClassificationRuleValidationException(findings);
        }
    }

    private List<MailMessage> fetchMessages(Inventory inventory) {
        Instant since = inventory.senders().stream()
                .map(sender -> sender.statistics())
                .map(InventoryStatistics::firstReceivedAt)
                .min(Instant::compareTo)
                .orElse(Instant.EPOCH);
        int maximum = inventory.senders().stream()
                .map(sender -> sender.statistics())
                .mapToInt(InventoryStatistics::messageCount)
                .sum();
        return messageSource.fetchSince(since, maximum);
    }

    private static List<MailMessage> displayMessages(
            List<MailMessage> messages, MailboxLabelCatalog labels) {
        return messages.stream().map(message -> new MailMessage(
                message.id(), message.sender(), message.subject(), message.receivedAt(),
                message.labels().stream().map(labels::nameForId).sorted().toList())).toList();
    }

    private static ClassificationDryRunReport report(
            RuleSet rules, List<MailMessage> messages, ClassificationPlan plan) {
        Map<String, MailMessage> messagesById = messages.stream().collect(Collectors.toMap(
                message -> message.id().value(), Function.identity()));
        Map<RuleId, RuleDefinition> rulesById = rules.rules().stream()
                .collect(Collectors.toMap(RuleDefinition::id, Function.identity()));
        ReportEntries entries = new ReportEntries(
                messagesById, rulesById, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        for (MessageLabelPlan messagePlan : plan.messages()) {
            classify(messagePlan, entries);
        }
        return new ClassificationDryRunReport(
                summary(plan, entries.changes(), entries.conflicts(), entries.unmatched()),
                entries.changes(), entries.conflicts(), entries.unmatched());
    }

    private static void classify(MessageLabelPlan plan, ReportEntries entries) {
        MailMessage message = entries.messages().get(plan.messageId().value());
        if (plan.matchedRuleIds().isEmpty()) {
            entries.unmatched().add(new ClassificationDryRunEntry(message, plan, NO_REASON));
        } else if (hasConflictingLabels(plan, entries.rules())) {
            entries.conflicts().add(
                    new ClassificationDryRunEntry(message, plan, CONFLICT_REASON));
        } else if (plan.changesLabels()) {
            entries.changes().add(new ClassificationDryRunEntry(message, plan, NO_REASON));
        }
    }

    private static boolean hasConflictingLabels(
            MessageLabelPlan plan, Map<RuleId, RuleDefinition> rules) {
        Set<String> labels = plan.matchedRuleIds().stream()
                .map(rules::get)
                .flatMap(rule -> rule.actions().stream())
                .map(RuleActionSpec::parameters)
                .map(parameters -> parameters.get("label"))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        return labels.size() > 1;
    }

    private static ClassificationDryRunSummary summary(
            ClassificationPlan plan,
            List<ClassificationDryRunEntry> changes,
            List<ClassificationDryRunEntry> conflicts,
            List<ClassificationDryRunEntry> unmatched) {
        int matched = plan.messages().size() - unmatched.size();
        int alreadyCorrect = matched - changes.size() - conflicts.size();
        int multiple = (int) plan.messages().stream()
                .filter(message -> message.matchedRuleIds().size() > 1).count();
        int additions = changes.stream().mapToInt(entry -> entry.plan().addLabels().size()).sum();
        int removals = changes.stream().mapToInt(entry -> entry.plan().removeLabels().size()).sum();
        return new ClassificationDryRunSummary(
                plan.messages().size(), matched, unmatched.size(), alreadyCorrect,
                changes.size(), additions, removals, multiple, conflicts.size());
    }

    private record ReportEntries(
            Map<String, MailMessage> messages,
            Map<RuleId, RuleDefinition> rules,
            List<ClassificationDryRunEntry> changes,
            List<ClassificationDryRunEntry> conflicts,
            List<ClassificationDryRunEntry> unmatched) {
    }
}
