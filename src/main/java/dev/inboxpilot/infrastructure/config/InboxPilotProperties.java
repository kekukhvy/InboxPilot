package dev.inboxpilot.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Root of InboxPilot's typed configuration, bound from the {@code inboxpilot.*}
 * keys in {@code application.yml}.
 *
 * <p>Every setting is validated at startup: if a value is missing, malformed, or
 * out of range, the application refuses to start and names the offending key
 * rather than failing later against a real mailbox. Configuration binding is an
 * infrastructure concern, so these types live outside the domain (ADR 0001).
 *
 * @param oauth       Google OAuth client and token storage
 * @param scanning    which messages a scan covers
 * @param batching    how requests are batched and retried
 * @param reports     where generated reports are written
 * @param checkpoints resumable-progress storage
 * @param ai          optional AI data-sharing controls
 */
@Validated
@ConfigurationProperties(prefix = InboxPilotProperties.PREFIX)
public record InboxPilotProperties(
        @Valid @NotNull OAuthProperties oauth,
        @Valid @NotNull ScanningProperties scanning,
        @Valid @NotNull BatchingProperties batching,
        @Valid @NotNull ReportProperties reports,
        @Valid @NotNull CheckpointProperties checkpoints,
        @Valid @NotNull AiProperties ai) {

    /** Prefix under which every InboxPilot setting is bound. */
    public static final String PREFIX = "inboxpilot";
}
