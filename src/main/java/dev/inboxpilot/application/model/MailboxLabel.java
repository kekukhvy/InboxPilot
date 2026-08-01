package dev.inboxpilot.application.model;

import java.util.Objects;

/**
 * Provider-independent mailbox label.
 *
 * @param id stable provider identifier
 * @param name user-visible label name
 */
public record MailboxLabel(String id, String name) {

    private static final String ID_PARAMETER = "id";
    private static final String NAME_PARAMETER = "name";
    private static final String BLANK_ID_MESSAGE = "Label id must not be blank";
    private static final String BLANK_NAME_MESSAGE = "Label name must not be blank";

    public MailboxLabel {
        Objects.requireNonNull(id, ID_PARAMETER);
        Objects.requireNonNull(name, NAME_PARAMETER);
        if (id.isBlank()) {
            throw new IllegalArgumentException(BLANK_ID_MESSAGE);
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException(BLANK_NAME_MESSAGE);
        }
    }
}
