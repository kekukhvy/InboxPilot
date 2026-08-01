package dev.inboxpilot.application.port;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Raised when a {@link CredentialProvider} cannot authorize access to the
 * mailbox — missing or invalid credentials, or a token store that cannot be
 * used. Mirrors {@link MessageSourceException}: one exception type per port,
 * so use cases handle a provider's error taxonomy in one place.
 */
@DisplayName("CredentialProviderException")
class CredentialProviderExceptionTest {

    private static final String MESSAGE = "inboxpilot.oauth.client-id must not be blank";

    @Test
    @DisplayName("keeps the message and an optional cause")
    void keepsMessageAndCause() {
        Throwable cause = new IllegalStateException("invalid_grant");

        CredentialProviderException failure = new CredentialProviderException(MESSAGE, cause);

        assertThat(failure.getMessage()).isEqualTo(MESSAGE);
        assertThat(failure.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("supports a message without a cause")
    void supportsMessageOnly() {
        CredentialProviderException failure = new CredentialProviderException(MESSAGE);

        assertThat(failure.getMessage()).isEqualTo(MESSAGE);
        assertThat(failure.getCause()).isNull();
    }

    @Test
    @DisplayName("is unchecked, so it can cross call sites that cannot recover")
    void isUnchecked() {
        assertThat(RuntimeException.class).isAssignableFrom(CredentialProviderException.class);
    }
}
