package dev.inboxpilot.application.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Bidirectional boundary between stable provider IDs and user-visible label names. */
public final class MailboxLabelCatalog {

    private static final String USER_LABEL_PREFIX = "Label_";
    private static final String UNKNOWN_LABEL_ID = "Unknown Gmail label id: ";
    private static final String UNKNOWN_LABEL_NAME = "Unknown Gmail label name: ";
    private static final String AMBIGUOUS_LABEL_NAME = "Ambiguous Gmail label name: ";

    private final Map<String, String> namesById;
    private final Map<String, List<String>> idsByName;

    public MailboxLabelCatalog(Collection<MailboxLabel> labels) {
        Objects.requireNonNull(labels, "labels");
        namesById = labels.stream().collect(Collectors.toUnmodifiableMap(
                MailboxLabel::id, MailboxLabel::name));
        idsByName = labels.stream().collect(Collectors.collectingAndThen(
                Collectors.groupingBy(
                        MailboxLabel::name,
                        Collectors.mapping(MailboxLabel::id, Collectors.toList())),
                values -> values.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> List.copyOf(entry.getValue())))));
    }

    public String nameForId(String id) {
        String name = namesById.get(id);
        if (name == null) {
            throw new IllegalArgumentException(UNKNOWN_LABEL_ID + id);
        }
        return name;
    }

    public String idForName(String name) {
        List<String> ids = idsByName.get(name);
        if (ids == null) {
            throw new IllegalArgumentException(UNKNOWN_LABEL_NAME + name);
        }
        if (ids.size() != 1) {
            throw new IllegalArgumentException(AMBIGUOUS_LABEL_NAME + name);
        }
        return ids.getFirst();
    }

    public Map<String, String> userLabelNamesById() {
        return namesById.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(USER_LABEL_PREFIX))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
