package dev.inboxpilot.infrastructure.report;

import dev.inboxpilot.domain.inventory.DomainInventory;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.inventory.SenderInventory;
import java.util.ArrayList;
import java.util.List;

final class InventoryCsvRenderer {

    private static final String HEADER =
            "type,key,messageCount,unreadCount,firstReceivedAt,lastReceivedAt,currentLabels,sampleSubjects";
    private static final String SENDER_TYPE = "sender";
    private static final String DOMAIN_TYPE = "domain";
    private static final String COMMA = ",";
    private static final String VALUE_SEPARATOR = "|";
    private static final String LINE_SEPARATOR = "\n";
    private static final String QUOTE = "\"";
    private static final String ESCAPED_QUOTE = "\"\"";

    String render(Inventory inventory) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        inventory.senders().stream().map(this::senderRow).forEach(lines::add);
        inventory.domains().stream().map(this::domainRow).forEach(lines::add);
        return String.join(LINE_SEPARATOR, lines) + LINE_SEPARATOR;
    }

    private String senderRow(SenderInventory inventory) {
        return row(SENDER_TYPE, inventory.sender().value(), inventory.statistics());
    }

    private String domainRow(DomainInventory inventory) {
        return row(DOMAIN_TYPE, inventory.domain(), inventory.statistics());
    }

    private String row(String type, String key, InventoryStatistics statistics) {
        return String.join(COMMA,
                escape(type), escape(key), Integer.toString(statistics.messageCount()),
                Integer.toString(statistics.unreadCount()),
                escape(statistics.firstReceivedAt().toString()),
                escape(statistics.lastReceivedAt().toString()),
                escape(String.join(VALUE_SEPARATOR, statistics.currentLabels())),
                escape(String.join(VALUE_SEPARATOR, statistics.sampleSubjects())));
    }

    private String escape(String value) {
        boolean needsQuotes = value.contains(COMMA) || value.contains(QUOTE)
                || value.contains(LINE_SEPARATOR);
        String escaped = value.replace(QUOTE, ESCAPED_QUOTE);
        return needsQuotes ? QUOTE + escaped + QUOTE : escaped;
    }
}
