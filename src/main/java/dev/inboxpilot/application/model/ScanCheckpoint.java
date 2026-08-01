package dev.inboxpilot.application.model;

import dev.inboxpilot.domain.message.MailMessage;
import java.util.List;
import java.util.Objects;

/** Resumable scan progress together with the messages aggregated so far. */
public record ScanCheckpoint(String fingerprint, List<MailMessage> messages) {

    public ScanCheckpoint {
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(messages, "messages");
        messages = List.copyOf(messages);
    }
}
