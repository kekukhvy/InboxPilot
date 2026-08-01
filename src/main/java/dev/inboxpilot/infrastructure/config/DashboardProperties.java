package dev.inboxpilot.infrastructure.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;

/** Configuration for the optional, local read-only dashboard. */
public record DashboardProperties(
        boolean enabled,
        @NotNull Path rulesFile,
        @Min(1) @Max(MAX_PREVIEW_BYTES) long maxPreviewBytes) {

    public static final long MAX_PREVIEW_BYTES = 10_485_760;
}
