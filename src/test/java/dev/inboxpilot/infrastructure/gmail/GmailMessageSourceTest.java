package dev.inboxpilot.infrastructure.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.application.port.MessageSourceException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the behaviour of the not-yet-implemented Gmail adapter.
 *
 * <p>The point of these tests is the failure mode, not the feature: until the
 * metadata retrieval lands (issue #11) the adapter must refuse loudly. Returning
 * an empty list would read as "the mailbox has no messages" and quietly corrupt
 * every inventory and analysis built on top of it — the opposite of the
 * "report what was degraded" invariant.
 */
@DisplayName("GmailMessageSource")
class GmailMessageSourceTest {

    private static final Instant SINCE = Instant.parse("2026-08-01T10:15:30Z");

    private final GmailMessageSource messageSource = new GmailMessageSource();

    @Test
    @DisplayName("fails loudly rather than reporting an empty mailbox")
    void failsRatherThanReturningEmpty() {
        assertThatExceptionOfType(MessageSourceException.class)
                .isThrownBy(() -> messageSource.fetchSince(SINCE))
                .withMessageContaining("not implemented");
    }

    @Test
    @DisplayName("names the metadata issue that will implement it, so the failure is actionable")
    void pointsAtTheMetadataIssue() {
        assertThatExceptionOfType(MessageSourceException.class)
                .isThrownBy(() -> messageSource.fetchSince(SINCE))
                .withMessageContaining("#11");
    }

    @Test
    @DisplayName("satisfies the port, so the composition root can wire it")
    void satisfiesThePort() {
        assertThat(messageSource).isInstanceOf(MessageSource.class);
    }
}
