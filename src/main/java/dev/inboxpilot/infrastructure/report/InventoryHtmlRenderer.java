package dev.inboxpilot.infrastructure.report;

import dev.inboxpilot.domain.inventory.DomainInventory;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.inventory.SenderInventory;
import java.util.List;
import java.util.function.Function;

final class InventoryHtmlRenderer {

    private static final String STYLE = "body{font-family:system-ui;margin:2rem;color:#17202a}nav a{margin-right:1rem}"
            + ".cards{display:flex;gap:1rem}.card{padding:1rem;border:1px solid #ccd6dd;border-radius:.5rem}"
            + "table{border-collapse:collapse;width:100%;margin-bottom:2rem}th,td{padding:.5rem;border:1px solid #ccd6dd}"
            + "th{cursor:pointer;background:#f4f6f7;text-align:left}";
    private static final String SORT_SCRIPT = "document.querySelectorAll('th').forEach((h,i)=>h.onclick=()=>{"
            + "const b=h.closest('table').tBodies[0];const r=[...b.rows];"
            + "r.sort((a,c)=>a.cells[i].textContent.localeCompare(c.cells[i].textContent,undefined,{numeric:true}));"
            + "r.forEach(x=>b.appendChild(x));});";

    String render(Inventory inventory) {
        int messages = inventory.senders().stream()
                .mapToInt(sender -> sender.statistics().messageCount())
                .sum();
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>InboxPilot analysis</title><style>" + STYLE + "</style></head><body>"
                + "<h1>InboxPilot analysis</h1><nav><a href=\"#senders\">Senders</a>"
                + "<a href=\"#domains\">Domains</a></nav>"
                + cards(inventory, messages)
                + table("senders", "Senders", inventory.senders(), this::senderRow)
                + table("domains", "Domains", inventory.domains(), this::domainRow)
                + "<script>" + SORT_SCRIPT + "</script></body></html>\n";
    }

    private String cards(Inventory inventory, int messages) {
        return "<section class=\"cards\" aria-label=\"Summary\">"
                + card("Messages", messages)
                + card("Senders", inventory.senders().size())
                + card("Domains", inventory.domains().size())
                + "</section>";
    }

    private String card(String label, int value) {
        return "<article class=\"card\"><strong>" + value + "</strong><div>" + label + "</div></article>";
    }

    private <T> String table(String id, String title, List<T> entries, Function<T, String> row) {
        String rows = String.join("", entries.stream().map(row).toList());
        return "<section id=\"" + id + "\"><h2>" + title + "</h2><table>"
                + "<thead><tr><th>Name</th><th>Messages</th><th>Unread</th><th>Labels</th></tr></thead>"
                + "<tbody>" + rows + "</tbody></table></section>";
    }

    private String senderRow(SenderInventory sender) {
        return row(sender.sender().value(), sender.statistics());
    }

    private String domainRow(DomainInventory domain) {
        return row(domain.domain(), domain.statistics());
    }

    private String row(String name, InventoryStatistics statistics) {
        return "<tr><td>" + escape(name) + "</td><td>" + statistics.messageCount()
                + "</td><td>" + statistics.unreadCount() + "</td><td>"
                + escape(String.join(", ", statistics.currentLabels())) + "</td></tr>";
    }

    private String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
