package dev.inboxpilot.application.port;

/** Reports an absent, unreadable, or version-incompatible message snapshot. */
public class MessageSnapshotStoreException extends RuntimeException {

    public MessageSnapshotStoreException(String message) {
        super(message);
    }

    public MessageSnapshotStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
