package dev.inboxpilot.infrastructure.gmail;

import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.application.port.MessageSourceException;
import dev.inboxpilot.domain.message.MailMessage;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Gmail adapter for the {@link MessageSource} port.
 *
 * <p>This is the one place allowed to know about Google client types: it calls
 * the Gmail API and translates the result into
 * {@link dev.inboxpilot.domain.message.MailMessage} values, so nothing further
 * in the application ever sees a Gmail type (ADR 0001, enforced by
 * {@code ArchitectureTest}).
 *
 * <p>Issue #10 supplies label and message-id discovery. Fetching the metadata
 * needed to construct a {@code MailMessage} belongs to issue #11; until then
 * this adapter fails loudly rather than pretending to return an empty mailbox.
 */
@Component
public class GmailMessageSource implements MessageSource {

    private static final Logger logger = LoggerFactory.getLogger(GmailMessageSource.class);

    private static final String NOT_IMPLEMENTED_MESSAGE =
            "Gmail message metadata retrieval is not implemented yet (see issue #11)";

    @Override
    public List<MailMessage> fetchSince(Instant since) {
        logger.debug("Gmail fetch requested for messages since {}", since);
        throw new MessageSourceException(NOT_IMPLEMENTED_MESSAGE);
    }
}
