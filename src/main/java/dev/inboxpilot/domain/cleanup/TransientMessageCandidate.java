package dev.inboxpilot.domain.cleanup;

import dev.inboxpilot.domain.message.MessageId;

/** Metadata-only transient cleanup candidate and matched reason. */
public record TransientMessageCandidate(
        MessageId messageId,
        TransientMessageCategory category,
        String reason) {}
