package dev.inboxpilot.application.cleanup;

import dev.inboxpilot.application.model.ArchiveMode;
import dev.inboxpilot.application.model.ArchivePlan;
import dev.inboxpilot.application.port.ArchiveGateway;
import dev.inboxpilot.domain.message.MessageId;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Previews selected archive candidates and executes only in explicit execute mode. */
public final class ArchiveService {

    private final ArchiveGateway gateway;

    public ArchiveService(ArchiveGateway gateway) {
        this.gateway = gateway;
    }

    public ArchivePlan run(Collection<MessageId> selectedMessageIds) {
        return run(selectedMessageIds, ArchiveMode.DRY_RUN);
    }

    public ArchivePlan run(
            Collection<MessageId> selectedMessageIds, ArchiveMode mode) {
        Objects.requireNonNull(mode, "mode");
        ArchivePlan plan = new ArchivePlan(List.copyOf(selectedMessageIds));
        if (mode == ArchiveMode.EXECUTE && !plan.messageIds().isEmpty()) {
            gateway.archive(plan.messageIds());
        }
        return plan;
    }
}
