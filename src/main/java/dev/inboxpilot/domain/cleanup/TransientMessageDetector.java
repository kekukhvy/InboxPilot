package dev.inboxpilot.domain.cleanup;

import dev.inboxpilot.domain.message.MailMessage;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Detects short-lived operational messages from their subjects. */
public final class TransientMessageDetector {

    private static final List<String> OTP_TERMS =
            List.of("verification code", "one-time code", "one time code", " otp", "passcode");
    private static final List<String> LOGIN_TERMS =
            List.of("login alert", "new login", "sign-in alert", "sign in alert");
    private static final List<String> DELIVERY_TERMS =
            List.of("was delivered", "out for delivery", "has shipped", "delivery status");
    private static final List<String> STATUS_TERMS =
            List.of("completed successfully", "status update", "request completed", "notification resolved");

    public List<TransientMessageCandidate> detect(Collection<MailMessage> messages) {
        return messages.stream()
                .map(TransientMessageDetector::classify)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(candidate -> candidate.messageId().value()))
                .toList();
    }

    private static Optional<TransientMessageCandidate> classify(MailMessage message) {
        String subject = message.subject().toLowerCase(Locale.ROOT);
        Optional<TransientMessageCategory> category = category(subject);
        return category.map(value -> new TransientMessageCandidate(
                message.id(), value, "subject matched " + value.name().toLowerCase(Locale.ROOT)));
    }

    private static Optional<TransientMessageCategory> category(String subject) {
        if (containsAny(subject, OTP_TERMS)) {
            return Optional.of(TransientMessageCategory.OTP);
        }
        if (containsAny(subject, LOGIN_TERMS)) {
            return Optional.of(TransientMessageCategory.LOGIN_ALERT);
        }
        if (containsAny(subject, DELIVERY_TERMS)) {
            return Optional.of(TransientMessageCategory.DELIVERY_STATUS);
        }
        if (containsAny(subject, STATUS_TERMS)) {
            return Optional.of(TransientMessageCategory.TRANSIENT_NOTIFICATION);
        }
        return Optional.empty();
    }

    private static boolean containsAny(String value, List<String> terms) {
        return terms.stream().anyMatch(value::contains);
    }
}
