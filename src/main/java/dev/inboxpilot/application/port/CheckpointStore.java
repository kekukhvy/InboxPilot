package dev.inboxpilot.application.port;

import dev.inboxpilot.application.model.ScanCheckpoint;
import java.util.Optional;

/** Persists and restores resumable scan state. */
public interface CheckpointStore {

    Optional<ScanCheckpoint> load(String expectedFingerprint);

    void save(ScanCheckpoint checkpoint);

    void reset();
}
