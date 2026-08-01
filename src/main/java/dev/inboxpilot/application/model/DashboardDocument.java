package dev.inboxpilot.application.model;

import java.util.Objects;

/** Preview content returned for a dashboard document selected by its opaque ID. */
public record DashboardDocument(String name, String mediaType, String content) {

    public DashboardDocument {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(mediaType, "mediaType");
        Objects.requireNonNull(content, "content");
    }
}
