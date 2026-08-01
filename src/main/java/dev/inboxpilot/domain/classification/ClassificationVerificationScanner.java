package dev.inboxpilot.domain.classification;

import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Compares planned final labels with freshly scanned post-execution metadata. */
public final class ClassificationVerificationScanner {

    public ClassificationVerificationReport verify(
            ClassificationPlan plan, Collection<MailMessage> freshMessages) {
        Map<MessageId, MailMessage> messagesById = freshMessages.stream()
                .collect(Collectors.toMap(MailMessage::id, Function.identity()));
        List<ClassificationVerificationEntry> entries = plan.messages().stream()
                .map(message -> verify(message, messagesById.get(message.messageId())))
                .toList();
        return new ClassificationVerificationReport(entries);
    }

    private static ClassificationVerificationEntry verify(
            MessageLabelPlan plan, MailMessage actualMessage) {
        if (actualMessage == null) {
            return new ClassificationVerificationEntry(
                    plan.messageId(), plan.newLabels(), List.of(), plan.newLabels(), List.of(),
                    ClassificationVerificationStatus.MESSAGE_MISSING);
        }
        Set<String> expected = Set.copyOf(plan.newLabels());
        Set<String> actual = Set.copyOf(actualMessage.labels());
        List<String> missing = difference(expected, actual);
        List<String> unexpected = difference(actual, expected);
        ClassificationVerificationStatus status = missing.isEmpty() && unexpected.isEmpty()
                ? ClassificationVerificationStatus.MATCH
                : ClassificationVerificationStatus.LABEL_MISMATCH;
        return new ClassificationVerificationEntry(
                plan.messageId(), sorted(expected), sorted(actual), missing, unexpected, status);
    }

    private static List<String> difference(Set<String> minuend, Set<String> subtrahend) {
        return minuend.stream().filter(value -> !subtrahend.contains(value)).sorted().toList();
    }

    private static List<String> sorted(Set<String> values) {
        return values.stream().sorted().toList();
    }
}
