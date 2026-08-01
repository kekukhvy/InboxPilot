package dev.inboxpilot.application.model;

import java.time.Instant;
import java.util.Objects;

/** Safe metadata for one report or rule document exposed by the dashboard. */
public record DashboardDocumentSummary(
        String id, String kind, String name, long size, Instant lastModified) {

    public DashboardDocumentSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lastModified, "lastModified");
    }
}
