package dev.inboxpilot.infrastructure.gmail;

import java.util.List;

record GmailMetadataBatch(
        List<GmailMessageMetadata> messages,
        List<GmailMetadataFailure> failures) {

    GmailMetadataBatch {
        messages = List.copyOf(messages);
        failures = List.copyOf(failures);
    }
}
