package dev.inboxpilot.cli;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain JUnit with a hand-written stub rather than a Spring context: that the
 * CLI can be exercised without one is the property ADR 0001 protects, and a
 * test that needed the real adapter would prove the boundary had leaked.
 */
@DisplayName("InboxPilotCommand")
class InboxPilotCommandTest {

    private final MessageSource stubSource = since -> List.of();

    @Test
    @DisplayName("reads through whichever port implementation it was given")
    void readsThroughTheInjectedPort() {
        InboxPilotCommand command = new InboxPilotCommand(stubSource);

        assertThat(command.messageSource()).isSameAs(stubSource);
    }

    @Test
    @DisplayName("depends on the port, not on any concrete adapter")
    void dependsOnThePortNotAnAdapter() {
        // Asserted against the constructor signature, because this is the one
        // property that keeps the CLI unaware of which provider is configured.
        Constructor<?>[] constructors = InboxPilotCommand.class.getDeclaredConstructors();

        assertThat(constructors).hasSize(1);
        assertThat(constructors[0].getParameterTypes()).containsExactly(MessageSource.class);
    }

    @Test
    @DisplayName("works with any port implementation, not just the Gmail one")
    void worksWithAnyImplementation() {
        MailMessage message = new MailMessage(
                new MessageId("msg-1"),
                new EmailAddress("news@example.com"),
                "Weekly digest",
                Instant.parse("2026-08-01T10:15:30Z"),
                List.of());
        MessageSource inMemorySource = since -> List.of(message);

        InboxPilotCommand command = new InboxPilotCommand(inMemorySource);
        command.reportConfiguredSource();

        assertThat(command.messageSource().fetchSince(Instant.EPOCH))
                .containsExactly(message);
    }
}
