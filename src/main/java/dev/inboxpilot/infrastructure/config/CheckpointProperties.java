package dev.inboxpilot.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;

/**
 * Resumable-progress storage.
 *
 * <p>A long scan that dies partway should resume rather than restart, so
 * progress is checkpointed every {@code interval} messages.
 *
 * @param enabled  whether progress is checkpointed at all
 * @param file     file holding the checkpoint state
 * @param interval messages processed between checkpoint writes
 */
public record CheckpointProperties(
        boolean enabled,
        @NotNull(message = "must be set: the file holding checkpoint state")
        Path file,
        @Min(value = MIN_INTERVAL, message = "must checkpoint after at least {value} message")
        int interval) {

    /** Checkpointing after every message is valid, if slow. */
    static final int MIN_INTERVAL = 1;
}
