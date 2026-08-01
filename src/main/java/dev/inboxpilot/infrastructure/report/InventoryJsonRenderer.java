package dev.inboxpilot.infrastructure.report;

import dev.inboxpilot.domain.inventory.DomainInventory;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.inventory.SenderInventory;
import java.util.List;
import java.util.function.Function;

final class InventoryJsonRenderer {

    private static final String QUOTE = "\"";
    private static final String ESCAPED_QUOTE = "\\\"";
    private static final String BACKSLASH = "\\";
    private static final String ESCAPED_BACKSLASH = "\\\\";
    private static final String COMMA = ",";
    private static final String COLON = ":";
    private static final String OBJECT_START = "{";
    private static final String OBJECT_END = "}";
    private static final String ARRAY_START = "[";
    private static final String ARRAY_END = "]";
    private static final String LINE_SEPARATOR = "\n";
    private static final String SENDERS_FIELD = "senders";
    private static final String DOMAINS_FIELD = "domains";
    private static final String SENDER_FIELD = "sender";
    private static final String DOMAIN_FIELD = "domain";
    private static final String MESSAGE_COUNT_FIELD = "messageCount";
    private static final String UNREAD_COUNT_FIELD = "unreadCount";
    private static final String FIRST_RECEIVED_FIELD = "firstReceivedAt";
    private static final String LAST_RECEIVED_FIELD = "lastReceivedAt";
    private static final String LABELS_FIELD = "currentLabels";
    private static final String SUBJECTS_FIELD = "sampleSubjects";

    String render(Inventory inventory) {
        return OBJECT_START + quoted(SENDERS_FIELD) + COLON
                + array(inventory.senders(), this::sender)
                + COMMA + quoted(DOMAINS_FIELD) + COLON
                + array(inventory.domains(), this::domain) + OBJECT_END + LINE_SEPARATOR;
    }

    private String sender(SenderInventory inventory) {
        return entry(SENDER_FIELD, inventory.sender().value(), inventory.statistics());
    }

    private String domain(DomainInventory inventory) {
        return entry(DOMAIN_FIELD, inventory.domain(), inventory.statistics());
    }

    private String entry(String keyName, String key, InventoryStatistics statistics) {
        return OBJECT_START + property(keyName, key)
                + COMMA + numericProperty(MESSAGE_COUNT_FIELD, statistics.messageCount())
                + COMMA + numericProperty(UNREAD_COUNT_FIELD, statistics.unreadCount())
                + COMMA + property(FIRST_RECEIVED_FIELD, statistics.firstReceivedAt().toString())
                + COMMA + property(LAST_RECEIVED_FIELD, statistics.lastReceivedAt().toString())
                + COMMA + arrayProperty(LABELS_FIELD, statistics.currentLabels())
                + COMMA + arrayProperty(SUBJECTS_FIELD, statistics.sampleSubjects()) + OBJECT_END;
    }

    private String property(String name, String value) {
        return quoted(name) + COLON + quoted(value);
    }

    private String numericProperty(String name, int value) {
        return quoted(name) + COLON + value;
    }

    private String arrayProperty(String name, List<String> values) {
        return quoted(name) + COLON + stringArray(values);
    }

    private String stringArray(List<String> values) {
        return ARRAY_START + String.join(COMMA, values.stream().map(this::quoted).toList())
                + ARRAY_END;
    }

    private <T> String array(List<T> values, Function<T, String> renderer) {
        return ARRAY_START + String.join(COMMA, values.stream().map(renderer).toList()) + ARRAY_END;
    }

    private String quoted(String value) {
        String escaped = value.replace(BACKSLASH, ESCAPED_BACKSLASH)
                .replace(QUOTE, ESCAPED_QUOTE);
        return QUOTE + escaped + QUOTE;
    }
}
