package dev.inboxpilot.domain.cleanup;

import dev.inboxpilot.domain.message.MessageId;

/** One deletion candidate with no executable mailbox operation. */
public record DeletionCandidate(
        MessageId messageId,
        DeletionCandidateCategory category,
        String reason) {}
