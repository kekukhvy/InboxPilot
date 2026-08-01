package dev.inboxpilot.domain.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Plain JUnit — no Spring context, no mailbox. That this test needs neither is
 * the property ADR 0001 exists to protect.
 */
@DisplayName("EmailAddress")
class EmailAddressTest {

    private static final String MIXED_CASE_ADDRESS = "News@Example.COM";
    private static final String NORMALISED_ADDRESS = "news@example.com";
    private static final String PADDED_ADDRESS = "  news@example.com  ";
    private static final String EXPECTED_DOMAIN = "example.com";
    private static final String SUBDOMAIN_ADDRESS = "alerts@mail.example.co.uk";
    private static final String EXPECTED_SUBDOMAIN = "mail.example.co.uk";

    @Test
    @DisplayName("normalises case so senders group regardless of how they were typed")
    void normalisesCase() {
        assertThat(new EmailAddress(MIXED_CASE_ADDRESS).value()).isEqualTo(NORMALISED_ADDRESS);
    }

    @Test
    @DisplayName("trims surrounding whitespace")
    void trimsWhitespace() {
        assertThat(new EmailAddress(PADDED_ADDRESS).value()).isEqualTo(NORMALISED_ADDRESS);
    }

    @Test
    @DisplayName("exposes the domain part")
    void exposesDomain() {
        assertThat(new EmailAddress(NORMALISED_ADDRESS).domain()).isEqualTo(EXPECTED_DOMAIN);
    }

    @Test
    @DisplayName("keeps every level of a multi-part domain")
    void exposesMultiLevelDomain() {
        assertThat(new EmailAddress(SUBDOMAIN_ADDRESS).domain()).isEqualTo(EXPECTED_SUBDOMAIN);
    }

    @ParameterizedTest(name = "rejects \"{0}\"")
    @DisplayName("rejects addresses without both a local and a domain part")
    @ValueSource(strings = {"", "   ", "no-at-sign", "@example.com", "local@", "@"})
    void rejectsMalformedAddresses(String malformed) {
        assertThatThrownBy(() -> new EmailAddress(malformed))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("local@domain");
    }

    @Test
    @DisplayName("rejects null")
    void rejectsNull() {
        assertThatThrownBy(() -> new EmailAddress(null))
                .isInstanceOf(NullPointerException.class);
    }
}
