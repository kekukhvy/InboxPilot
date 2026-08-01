package dev.inboxpilot.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.inventory.DomainInventory;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.inventory.SenderInventory;
import dev.inboxpilot.domain.message.EmailAddress;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Inventory report renderers")
class InventoryReportRendererTest {

    private static final String SENDER = "news@example.com";
    private static final String DOMAIN = "example.com";
    private static final String SUBJECT = "Digest, \"weekly\"";
    private static final String LABEL = "INBOX";
    private static final Instant RECEIVED_AT = Instant.parse("2026-08-01T10:00:00Z");
    private static final String CSV_ESCAPED_SUBJECT = "\"Digest, \"\"weekly\"\"\"";
    private static final String JSON_ESCAPED_SUBJECT = "Digest, \\\"weekly\\\"";
    private static final String UNSAFE_LABEL = "<script>alert('x')</script>";

    @Test
    @DisplayName("CSV is deterministic and escapes spreadsheet fields")
    void rendersDeterministicCsv() {
        InventoryCsvRenderer renderer = new InventoryCsvRenderer();

        String first = renderer.render(inventory());
        String second = renderer.render(inventory());

        assertThat(first).isEqualTo(second).contains(CSV_ESCAPED_SUBJECT, SENDER, DOMAIN);
    }

    @Test
    @DisplayName("JSON is deterministic and escapes string values")
    void rendersDeterministicJson() {
        InventoryJsonRenderer renderer = new InventoryJsonRenderer();

        String first = renderer.render(inventory());
        String second = renderer.render(inventory());

        assertThat(first).isEqualTo(second).contains(JSON_ESCAPED_SUBJECT, SENDER, DOMAIN);
    }

    @Test
    @DisplayName("HTML has navigation, summary cards, sortable tables, and escaped values")
    void rendersNavigableHtml() {
        InventoryStatistics statistics = new InventoryStatistics(
                1, 1, RECEIVED_AT, RECEIVED_AT, List.of(UNSAFE_LABEL), List.of(SUBJECT));
        Inventory inventory = new Inventory(
                List.of(new SenderInventory(new EmailAddress(SENDER), statistics)),
                List.of(new DomainInventory(DOMAIN, statistics)));

        String html = new InventoryHtmlRenderer().render(inventory);

        assertThat(html)
                .contains("href=\"#senders\"", "class=\"cards\"", "<table>", "localeCompare")
                .contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;")
                .doesNotContain(UNSAFE_LABEL);
    }

    private static Inventory inventory() {
        InventoryStatistics statistics = new InventoryStatistics(
                1, 1, RECEIVED_AT, RECEIVED_AT, List.of(LABEL), List.of(SUBJECT));
        return new Inventory(
                List.of(new SenderInventory(new EmailAddress(SENDER), statistics)),
                List.of(new DomainInventory(DOMAIN, statistics)));
    }
}
