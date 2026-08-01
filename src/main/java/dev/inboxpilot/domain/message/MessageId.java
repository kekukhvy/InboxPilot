package dev.inboxpilot.domain.message;

import java.util.Objects;

/**
 * Provider-independent identifier of a mail message.
 *
 * <p>A wrapper rather than a bare {@code String} so a message id cannot be
 * passed where a label or an address is expected.
 *
 * @param value the raw identifier as issued by the mail provider
 */
public record MessageId(String value) {

    /**
     * @throws NullPointerException     if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is blank
     */
    public MessageId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Message id must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
