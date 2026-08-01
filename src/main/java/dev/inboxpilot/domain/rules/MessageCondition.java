package dev.inboxpilot.domain.rules;

import dev.inboxpilot.domain.message.MailMessage;

/** Executable provider-independent condition over message metadata. */
@FunctionalInterface
public interface MessageCondition {

    boolean matches(MailMessage message);
}
