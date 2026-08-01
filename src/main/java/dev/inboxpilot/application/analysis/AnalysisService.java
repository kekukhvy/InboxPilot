package dev.inboxpilot.application.analysis;

import dev.inboxpilot.application.model.MailboxLabel;
import dev.inboxpilot.application.port.MailboxGateway;
import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.domain.analysis.LabelDefinition;
import dev.inboxpilot.domain.analysis.LabelStructureAnalysis;
import dev.inboxpilot.domain.analysis.LabelStructureAnalyzer;
import dev.inboxpilot.domain.analysis.UnclassifiedInventory;
import dev.inboxpilot.domain.analysis.UnclassifiedInventoryAnalyzer;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryAggregator;
import dev.inboxpilot.domain.message.MailMessage;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Orchestrates deterministic, read-only mailbox analysis. */
public final class AnalysisService {

    private static final String USER_LABEL_PREFIX = "Label_";

    private final MessageSource messageSource;
    private final MailboxGateway mailboxGateway;
    private final Duration lookback;
    private final int maximumMessages;
    private final Clock clock;
    private final InventoryAggregator inventoryAggregator = new InventoryAggregator();
    private final UnclassifiedInventoryAnalyzer unclassifiedAnalyzer =
            new UnclassifiedInventoryAnalyzer();
    private final LabelStructureAnalyzer labelStructureAnalyzer = new LabelStructureAnalyzer();

    public AnalysisService(
            MessageSource messageSource,
            MailboxGateway mailboxGateway,
            Duration lookback,
            int maximumMessages,
            Clock clock) {
        this.messageSource = Objects.requireNonNull(messageSource, "messageSource");
        this.mailboxGateway = Objects.requireNonNull(mailboxGateway, "mailboxGateway");
        this.lookback = Objects.requireNonNull(lookback, "lookback");
        this.maximumMessages = maximumMessages;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AnalysisRunResult run() {
        List<MailMessage> messages = messageSource.fetchSince(
                clock.instant().minus(lookback), maximumMessages);
        Inventory inventory = inventoryAggregator.aggregate(messages);
        List<MailboxLabel> userLabels = mailboxGateway.listLabels().stream()
                .filter(AnalysisService::isUserLabel)
                .toList();
        Set<String> userLabelIds = userLabels.stream()
                .map(MailboxLabel::id)
                .collect(Collectors.toUnmodifiableSet());
        UnclassifiedInventory unclassified = unclassifiedAnalyzer.analyze(
                inventory, userLabelIds);
        LabelStructureAnalysis structure = labelStructureAnalyzer.analyze(
                labelDefinitions(userLabels), inventory);
        return result(messages, unclassified, structure);
    }

    private static boolean isUserLabel(MailboxLabel label) {
        return label.id().startsWith(USER_LABEL_PREFIX);
    }

    private static List<LabelDefinition> labelDefinitions(List<MailboxLabel> labels) {
        return labels.stream()
                .map(label -> new LabelDefinition(label.id(), label.name()))
                .toList();
    }

    private static AnalysisRunResult result(
            List<MailMessage> messages,
            UnclassifiedInventory unclassified,
            LabelStructureAnalysis structure) {
        return new AnalysisRunResult(
                messages.size(),
                unclassified.senders().size(),
                unclassified.domains().size(),
                structure.unusedOrEmptyLabels().size(),
                structure.duplicateLabelGroups().size());
    }
}
