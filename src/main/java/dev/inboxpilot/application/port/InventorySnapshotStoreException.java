package dev.inboxpilot.application.port;

/** Reports an absent, unreadable, or malformed persisted inventory. */
public class InventorySnapshotStoreException extends RuntimeException {

    public InventorySnapshotStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
