package dev.inboxpilot.domain.analysis;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MessageId;
import java.time.Instant;

/** One message proposed for review, never an instruction to mutate it. */
public record CleanupCandidate(
        MessageId id,
        EmailAddress sender,
        String subject,
        Instant receivedAt,
        CleanupCategory category) {}
