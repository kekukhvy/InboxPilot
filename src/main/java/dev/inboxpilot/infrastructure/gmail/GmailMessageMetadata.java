package dev.inboxpilot.infrastructure.gmail;

import java.time.Instant;
import java.util.List;

record GmailMessageMetadata(
        String id,
        String sender,
        String subject,
        Instant receivedAt,
        List<String> labelIds) {

    GmailMessageMetadata {
        labelIds = List.copyOf(labelIds);
    }
}
