package dev.inboxpilot.application.port;

import dev.inboxpilot.application.model.DashboardDocument;
import dev.inboxpilot.application.model.DashboardDocumentSummary;
import java.util.List;
import java.util.Optional;

/** Read-only access to documents that may be reviewed in the dashboard. */
public interface DashboardDocumentStore {

    List<DashboardDocumentSummary> list();

    Optional<DashboardDocument> findById(String id);
}
