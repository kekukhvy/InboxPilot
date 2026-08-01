package dev.inboxpilot.application.dashboard;

/** Raised when a dashboard document ID is unknown or no longer available. */
public final class DashboardDocumentNotFoundException extends RuntimeException {

    public DashboardDocumentNotFoundException(String id) {
        super("Dashboard document not found: " + id);
    }
}
