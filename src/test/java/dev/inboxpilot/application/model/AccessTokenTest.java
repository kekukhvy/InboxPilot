package dev.inboxpilot.application.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A provider-independent, domain-terms authorization result — never a Google
 * {@code Credential} — so callers outside {@code infrastructure.gmail} never
 * see a Google client type (ADR 0001).
 */
@DisplayName("AccessToken")
class AccessTokenTest {

    private static final String VALUE = "ya29.example-access-token";
    private static final Instant EXPIRES_AT = Instant.parse("2026-08-01T11:00:00Z");
    private static final Instant NOW = Instant.parse("2026-08-01T10:00:00Z");
    private static final Instant PAST = Instant.parse("2026-08-01T09:00:00Z");

    @Test
    @DisplayName("rejects a null value")
    void rejectsNullValue() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AccessToken(null, EXPIRES_AT))
                .withMessageContaining("value");
    }

    @Test
    @DisplayName("rejects a blank value")
    void rejectsBlankValue() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AccessToken("  ", EXPIRES_AT))
                .withMessageContaining("blank");
    }

    @Test
    @DisplayName("rejects a null expiry")
    void rejectsNullExpiry() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AccessToken(VALUE, null))
                .withMessageContaining("expiresAt");
    }

    @Test
    @DisplayName("is not expired before its expiry instant")
    void isNotExpiredBeforeExpiry() {
        AccessToken token = new AccessToken(VALUE, EXPIRES_AT);

        assertThat(token.isExpiredAt(NOW)).isFalse();
    }

    @Test
    @DisplayName("is expired at or after its expiry instant")
    void isExpiredAtOrAfterExpiry() {
        AccessToken token = new AccessToken(VALUE, PAST);

        assertThat(token.isExpiredAt(NOW)).isTrue();
    }
}
