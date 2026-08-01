package dev.inboxpilot.infrastructure.report;

import dev.inboxpilot.domain.classification.RollbackLabelChange;
import dev.inboxpilot.domain.classification.RollbackPlan;
import java.util.List;

/** Renders a versioned rollback plan as deterministic JSON. */
public final class RollbackPlanJsonRenderer {

    public String render(RollbackPlan plan) {
        String changes = String.join(",", plan.changes().stream().map(this::change).toList());
        return "{\"version\":" + plan.version() + ",\"changes\":[" + changes + "]}\n";
    }

    private String change(RollbackLabelChange change) {
        return "{\"messageId\":" + quoted(change.messageId().value())
                + ",\"addLabelIds\":" + array(change.addLabelIds())
                + ",\"removeLabelIds\":" + array(change.removeLabelIds()) + "}";
    }

    private String array(List<String> values) {
        return "[" + String.join(",", values.stream().map(this::quoted).toList()) + "]";
    }

    private String quoted(String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }
}
