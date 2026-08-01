package dev.inboxpilot.infrastructure.gmail;

record GmailMetadataFailure(
        String messageId,
        String reason,
        boolean retryable,
        boolean throttled) {
}
