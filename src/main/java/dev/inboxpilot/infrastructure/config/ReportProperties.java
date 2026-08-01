package dev.inboxpilot.infrastructure.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.List;

/**
 * Where generated reports are written and in which formats.
 *
 * @param outputDirectory directory receiving generated reports
 * @param formats         formats to emit; at least one
 * @param overwrite       whether an existing report file may be replaced
 */
public record ReportProperties(
        @NotNull(message = "must be set: the directory reports are written to")
        Path outputDirectory,
        @NotEmpty(message = "must list at least one format (JSON, CSV, HTML)")
        List<@NotNull(message = "must be one of JSON, CSV, HTML") ReportFormat> formats,
        boolean overwrite) {

    /**
     * Copies the format list so configuration cannot be mutated after binding.
     */
    public ReportProperties {
        if (formats != null) {
            formats = List.copyOf(formats);
        }
    }
}
