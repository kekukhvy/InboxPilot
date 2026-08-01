package dev.inboxpilot.domain.ai;

import dev.inboxpilot.domain.message.EmailAddress;
import java.util.List;

/** Local metadata considered for privacy-preserving AI projection. */
public record AiMessageMetadata(
        EmailAddress sender,
        String subject,
        List<String> labelIds) {

    public AiMessageMetadata {
        labelIds = List.copyOf(labelIds);
    }
}
