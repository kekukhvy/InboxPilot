package dev.inboxpilot.domain.analysis;

import dev.inboxpilot.domain.message.EmailAddress;
import java.util.List;

/** Sender classified as newsletter or bulk mail with explainable evidence. */
public record BulkMailSender(EmailAddress sender, int messageCount, List<String> signals) {

    public BulkMailSender {
        signals = List.copyOf(signals);
    }
}
