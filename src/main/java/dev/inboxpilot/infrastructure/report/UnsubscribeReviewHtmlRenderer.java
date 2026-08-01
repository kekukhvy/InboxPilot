package dev.inboxpilot.infrastructure.report;

import dev.inboxpilot.domain.cleanup.UnsubscribeReviewEntry;
import dev.inboxpilot.domain.cleanup.UnsubscribeReviewReport;
import java.net.URI;

/** Renders manual unsubscribe links without invoking any capability. */
public final class UnsubscribeReviewHtmlRenderer {

    public String render(UnsubscribeReviewReport report) {
        String entries = String.join("", report.entries().stream().map(this::entry).toList());
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<title>Unsubscribe review</title></head><body><h1>Unsubscribe review</h1>"
                + "<p>Review each capability manually. InboxPilot has not invoked any link.</p>"
                + entries + "</body></html>\n";
    }

    private String entry(UnsubscribeReviewEntry entry) {
        String mailto = String.join("", entry.mailto().stream()
                .map(uri -> link("Email unsubscribe", uri)).toList());
        String https = String.join("", entry.https().stream()
                .map(uri -> link("Web unsubscribe", uri)).toList());
        return "<section><h2>" + escape(entry.sender().value()) + "</h2><ul>"
                + mailto + https + "</ul></section>";
    }

    private String link(String label, URI uri) {
        String value = escape(uri.toASCIIString());
        return "<li><a rel=\"noreferrer\" href=\"" + value + "\">"
                + label + ": " + value + "</a></li>";
    }

    private String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
