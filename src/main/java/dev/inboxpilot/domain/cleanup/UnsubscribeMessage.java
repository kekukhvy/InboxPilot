package dev.inboxpilot.domain.cleanup;

import dev.inboxpilot.domain.analysis.UnsubscribeCapabilities;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MessageId;

/** Message-level unsubscribe capabilities prepared for manual review. */
public record UnsubscribeMessage(
        MessageId messageId,
        EmailAddress sender,
        UnsubscribeCapabilities capabilities) {}
