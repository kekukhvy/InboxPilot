package dev.inboxpilot.application.analysis;

import dev.inboxpilot.domain.message.EmailAddress;

/** Aggregate sender evidence that may deserve a cleanup rule. */
public record AnalysisCleanupCandidate(
        EmailAddress sender, int messageCount, int unreadCount, String reason) {
}
