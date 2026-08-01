package dev.inboxpilot.application.port;

/** Reports unreadable, unwritable, or incompatible checkpoint state. */
public class CheckpointStoreException extends RuntimeException {

    public CheckpointStoreException(String message) {
        super(message);
    }

    public CheckpointStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
