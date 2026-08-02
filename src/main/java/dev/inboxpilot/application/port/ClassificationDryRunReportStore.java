package dev.inboxpilot.application.port;

import dev.inboxpilot.application.classification.ClassificationDryRunReport;
import java.nio.file.Path;
import java.util.List;

/** Persists read-only classification simulation reports. */
public interface ClassificationDryRunReportStore {

    List<Path> write(ClassificationDryRunReport report);
}
