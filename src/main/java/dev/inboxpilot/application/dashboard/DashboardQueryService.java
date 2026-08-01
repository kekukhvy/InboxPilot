package dev.inboxpilot.application.dashboard;

import dev.inboxpilot.application.model.DashboardDocument;
import dev.inboxpilot.application.model.DashboardDocumentSummary;
import dev.inboxpilot.application.port.DashboardDocumentStore;
import java.util.List;
import java.util.Objects;

/** Provides the dashboard's deliberately read-only report and rule queries. */
public final class DashboardQueryService {

    private final DashboardDocumentStore store;

    public DashboardQueryService(DashboardDocumentStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public List<DashboardDocumentSummary> list() {
        return store.list();
    }

    public DashboardDocument get(String id) {
        return store.findById(id)
                .orElseThrow(() -> new DashboardDocumentNotFoundException(id));
    }
}
