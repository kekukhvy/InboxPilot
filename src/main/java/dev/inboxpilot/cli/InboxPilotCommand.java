package dev.inboxpilot.cli;

import dev.inboxpilot.application.port.MessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class InboxPilotCommand {

    private static final Logger logger = LoggerFactory.getLogger(InboxPilotCommand.class);

    private final MessageSource messageSource;

    /**
     * @param messageSource the port used to read the mailbox; injected by the
     *                      composition root with whichever adapter is active
     */
    public InboxPilotCommand(MessageSource messageSource) {
        this.messageSource = messageSource;
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
        logger.info("InboxPilot CLI ready, reading messages via {}",
                messageSource.getClass().getSimpleName());
    }
}
