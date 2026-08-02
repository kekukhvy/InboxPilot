package dev.inboxpilot.application.classification;

import dev.inboxpilot.domain.classification.MessageLabelPlan;
import dev.inboxpilot.domain.message.MailMessage;

/** Message metadata paired with its provider-neutral dry-run plan. */
public record ClassificationDryRunEntry(
        MailMessage message, MessageLabelPlan plan, String reason) {
}
