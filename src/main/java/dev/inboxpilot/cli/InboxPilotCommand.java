package dev.inboxpilot.cli;

import dev.inboxpilot.application.model.MailboxLabel;
import dev.inboxpilot.application.analysis.AnalysisRunResult;
import dev.inboxpilot.application.analysis.AnalysisService;
import dev.inboxpilot.application.inventory.InventoryRunResult;
import dev.inboxpilot.application.inventory.InventoryService;
import dev.inboxpilot.application.port.MailboxGateway;
import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.application.port.CheckpointStore;
import dev.inboxpilot.domain.message.MessageId;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Command-line entry point.
 *
 * <p>Translates command-line input into use-case calls and renders the result;
 * it holds no business logic of its own. It depends on the
 * {@link MessageSource} port rather than on any adapter, so the CLI is unaware
 * of which mail provider is configured (ADR 0001).
 *
 * <p>The concrete commands (inventory, analyse, classify) arrive with their own
 * issues; this type establishes the layer and its dependency direction.
 */
@Component
public class InboxPilotCommand implements ApplicationRunner {

    private static final String LABELS_COMMAND = "labels";
    private static final String MESSAGES_COMMAND = "messages";
    private static final String CHECKPOINT_COMMAND = "checkpoint";
    private static final String INVENTORY_COMMAND = "inventory";
    private static final String ANALYZE_COMMAND = "analyze";
    private static final String INVENTORY_SUMMARY = "Inventory complete: %d messages";
    private static final String ANALYSIS_SUMMARY =
            "Analysis complete: %d messages, %d unclassified senders, "
                    + "%d unclassified domains";
    private static final String REPORT_PREFIX = "Report: ";
    private static final String RESET_ACTION = "reset";
    private static final String RESET_CONFIRMATION = "Checkpoint reset";
    private static final String LIST_ACTION = "list";
    private static final String OPTION_PREFIX = "--";
    private static final String QUERY_PREFIX = OPTION_PREFIX + "query=";
    private static final String LABEL_SEPARATOR = "\t";
    private static final String OUTPUT_TEMPLATE = "{}";
    private static final String READY_TEMPLATE = "InboxPilot CLI ready, reading messages via {}";
    private static final String USAGE =
            "Usage: inventory | analyze | labels list | messages list --query=<gmail-query> "
                    + "| checkpoint reset";
    private static final int COMMAND_INDEX = 0;
    private static final int ACTION_INDEX = 1;
    private static final int REQUIRED_COMMAND_PARTS = 2;

    private static final Logger logger = LoggerFactory.getLogger(InboxPilotCommand.class);

    private final MessageSource messageSource;
    private final MailboxGateway mailboxGateway;
    private final CheckpointStore checkpointStore;
    private final InventoryService inventoryService;
    private final AnalysisService analysisService;

    /**
     * @param messageSource  the port used to retrieve complete messages
     * @param mailboxGateway the port used to discover labels and message ids
     */
    public InboxPilotCommand(
            MessageSource messageSource,
            MailboxGateway mailboxGateway,
            CheckpointStore checkpointStore,
            InventoryService inventoryService,
            AnalysisService analysisService) {
        this.messageSource = messageSource;
        this.mailboxGateway = mailboxGateway;
        this.checkpointStore = checkpointStore;
        this.inventoryService = inventoryService;
        this.analysisService = analysisService;
    }

    /**
     * Returns the message source this command reads from.
     *
     * @return the injected port implementation
     */
    public MessageSource messageSource() {
        return messageSource;
    }

    /**
     * Logs which adapter the CLI was wired with. Replaced by real command
     * dispatch in a later slice.
     */
    public void reportConfiguredSource() {
        logger.info(READY_TEMPLATE, messageSource.getClass().getSimpleName());
    }

    @Override
    public void run(ApplicationArguments arguments) {
        List<String> output = execute(arguments.getSourceArgs());
        output.forEach(line -> logger.info(OUTPUT_TEMPLATE, line));
    }

    /**
     * Executes a read-only mailbox command and renders its result as lines.
     *
     * @param arguments command-line arguments
     * @return lines ready for CLI output
     */
    public List<String> execute(String... arguments) {
        List<String> commandParts = commandParts(arguments);
        if (commandParts.isEmpty()) {
            reportConfiguredSource();
            return List.of();
        }
        if (isCommand(commandParts, LABELS_COMMAND)) {
            return mailboxGateway.listLabels().stream().map(InboxPilotCommand::renderLabel).toList();
        }
        if (commandParts.equals(List.of(INVENTORY_COMMAND))) {
            return renderInventory(inventoryService.run());
        }
        if (commandParts.equals(List.of(ANALYZE_COMMAND))) {
            return renderAnalysis(analysisService.run());
        }
        if (isCommand(commandParts, MESSAGES_COMMAND)) {
            return mailboxGateway.listMessageIds(requireQuery(arguments)).stream()
                    .map(MessageId::value)
                    .toList();
        }
        if (isAction(commandParts, CHECKPOINT_COMMAND, RESET_ACTION)) {
            checkpointStore.reset();
            return List.of(RESET_CONFIRMATION);
        }
        throw new IllegalArgumentException(USAGE);
    }

    private static List<String> commandParts(String[] arguments) {
        return Arrays.stream(arguments)
                .filter(argument -> !argument.startsWith(OPTION_PREFIX))
                .toList();
    }

    private static boolean isCommand(List<String> arguments, String command) {
        return isAction(arguments, command, LIST_ACTION);
    }

    private static boolean isAction(List<String> arguments, String command, String action) {
        return arguments.size() >= REQUIRED_COMMAND_PARTS
                && command.equals(arguments.get(COMMAND_INDEX))
                && action.equals(arguments.get(ACTION_INDEX));
    }

    private static String requireQuery(String[] arguments) {
        return Arrays.stream(arguments)
                .filter(argument -> argument.startsWith(QUERY_PREFIX))
                .map(argument -> argument.substring(QUERY_PREFIX.length()))
                .findFirst()
                .filter(query -> !query.isBlank())
                .orElseThrow(() -> new IllegalArgumentException(USAGE));
    }

    private static String renderLabel(MailboxLabel label) {
        return label.id() + LABEL_SEPARATOR + label.name();
    }

    private static List<String> renderInventory(InventoryRunResult result) {
        List<String> reports = result.reports().stream()
                .map(path -> REPORT_PREFIX + path.toAbsolutePath())
                .toList();
        java.util.ArrayList<String> output = new java.util.ArrayList<>();
        output.add(INVENTORY_SUMMARY.formatted(result.processedMessages()));
        output.addAll(reports);
        return List.copyOf(output);
    }

    private static List<String> renderAnalysis(AnalysisRunResult result) {
        java.util.ArrayList<String> output = new java.util.ArrayList<>();
        output.add(ANALYSIS_SUMMARY.formatted(
                result.processedMessages(),
                result.unclassifiedSenders(),
                result.unclassifiedDomains()));
        result.reports().stream()
                .map(path -> REPORT_PREFIX + path.toAbsolutePath())
                .forEach(output::add);
        return List.copyOf(output);
    }
}
