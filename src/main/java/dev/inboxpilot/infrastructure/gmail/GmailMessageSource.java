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
 * <p>The Gmail calls themselves arrive with issue #9 (desktop OAuth) and #10
 * (gateway); until then this adapter exists to anchor the boundary and fails
 * loudly rather than pretending to return an empty mailbox — an empty result
 * would read as "no messages" and silently corrupt any analysis built on it.
 */
@Component
public class GmailMessageSource implements MessageSource {

    private static final Logger logger = LoggerFactory.getLogger(GmailMessageSource.class);

    private static final String NOT_IMPLEMENTED_MESSAGE =
            "Gmail message retrieval is not implemented yet (see issues #9 and #10)";

    @Override
    public List<MailMessage> fetchSince(Instant since) {
        logger.debug("Gmail fetch requested for messages since {}", since);
        throw new MessageSourceException(NOT_IMPLEMENTED_MESSAGE);
    }
}
