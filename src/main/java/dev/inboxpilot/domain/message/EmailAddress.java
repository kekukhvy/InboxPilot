package dev.inboxpilot.domain.message;

import java.util.Locale;
import java.util.Objects;

/**
 * An email address, normalised to lower case.
 *
 * <p>Carries the domain part as a first-class concept because sender-domain
 * grouping drives much of InboxPilot's analysis (top senders, newsletter
 * detection, per-domain rules).
 *
 * @param value the normalised address, e.g. {@code news@example.com}
 */
public record EmailAddress(String value) {

    private static final char DOMAIN_SEPARATOR = '@';

    /**
     * @throws NullPointerException     if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is not of the form
     *                                  {@code local@domain}
     */
    public EmailAddress {
        Objects.requireNonNull(value, "value");
        value = value.trim().toLowerCase(Locale.ROOT);
        int separator = value.indexOf(DOMAIN_SEPARATOR);
        boolean hasLocalPart = separator > 0;
        boolean hasDomainPart = separator >= 0 && separator < value.length() - 1;
        if (!hasLocalPart || !hasDomainPart) {
            throw new IllegalArgumentException(
                    "Email address must be of the form local@domain, but was: " + value);
        }
    }

    /**
     * Returns the domain part of the address.
     *
     * @return everything after the {@code @}, e.g. {@code example.com}
     */
    public String domain() {
        return value.substring(value.indexOf(DOMAIN_SEPARATOR) + 1);
    }

    @Override
    public String toString() {
        return value;
    }
}
