package dev.inboxpilot.infrastructure.report;

import dev.inboxpilot.domain.classification.ClassificationAuditEntry;
import dev.inboxpilot.domain.classification.ClassificationAuditReport;

/** Renders classification audit records as deterministic escaped CSV. */
public final class ClassificationAuditCsvRenderer {

    private static final String HEADER = "ruleId,messageId,oldLabels,plannedLabels,newLabels,result\n";

    public String render(ClassificationAuditReport report) {
        StringBuilder csv = new StringBuilder(HEADER);
        report.entries().forEach(entry -> csv.append(row(entry)));
        return csv.toString();
    }

    private static String row(ClassificationAuditEntry entry) {
        return String.join(",",
                field(entry.ruleId().value()),
                field(entry.messageId().value()),
                field(String.join("|", entry.oldLabels())),
                field(String.join("|", entry.plannedLabels())),
                field(String.join("|", entry.newLabels())),
                field(entry.result().name())) + "\n";
    }

    private static String field(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
