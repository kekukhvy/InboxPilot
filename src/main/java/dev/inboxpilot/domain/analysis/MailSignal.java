package dev.inboxpilot.domain.analysis;

import dev.inboxpilot.domain.message.EmailAddress;
import java.util.Map;
import java.util.Objects;

/** Header evidence used to classify one message without inspecting its body. */
public record MailSignal(EmailAddress sender, Map<String, String> headers) {

    public MailSignal {
        Objects.requireNonNull(sender, "sender");
        headers = Map.copyOf(headers);
    }
}
