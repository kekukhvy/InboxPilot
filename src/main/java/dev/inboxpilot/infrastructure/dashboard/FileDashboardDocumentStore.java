package dev.inboxpilot.infrastructure.dashboard;

import dev.inboxpilot.application.model.DashboardDocument;
import dev.inboxpilot.application.model.DashboardDocumentSummary;
import dev.inboxpilot.application.port.DashboardDocumentStore;
import dev.inboxpilot.infrastructure.config.DashboardProperties;
import dev.inboxpilot.infrastructure.config.ReportProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** Filesystem adapter that exposes only configured reports and one rule file. */
public final class FileDashboardDocumentStore implements DashboardDocumentStore {

    private static final String REPORT = "REPORT";
    private static final String RULES = "RULES";
    private static final Set<String> REPORT_EXTENSIONS = Set.of("json", "csv", "html");
    private static final Set<String> RULE_EXTENSIONS = Set.of("yaml", "yml");
    private static final Comparator<DashboardDocumentSummary> DOCUMENT_ORDER =
            Comparator.comparing(DashboardDocumentSummary::kind)
                    .thenComparing(DashboardDocumentSummary::name);

    private final Path reportDirectory;
    private final Path rulesFile;
    private final long maximumBytes;

    public FileDashboardDocumentStore(
            ReportProperties reports, DashboardProperties dashboard) {
        reportDirectory = reports.outputDirectory().toAbsolutePath().normalize();
        rulesFile = dashboard.rulesFile().toAbsolutePath().normalize();
        maximumBytes = dashboard.maxPreviewBytes();
    }

    @Override
    public List<DashboardDocumentSummary> list() {
        List<DashboardDocumentSummary> documents = new ArrayList<>(reportDocuments());
        summary(rulesFile, RULES, RULE_EXTENSIONS).ifPresent(documents::add);
        return documents.stream().sorted(DOCUMENT_ORDER).toList();
    }

    @Override
    public Optional<DashboardDocument> findById(String id) {
        return list().stream()
                .filter(summary -> summary.id().equals(id))
                .findFirst()
                .flatMap(this::read);
    }

    private List<DashboardDocumentSummary> reportDocuments() {
        if (!Files.isDirectory(reportDirectory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(reportDirectory)) {
            return files.map(path -> summary(path, REPORT, REPORT_EXTENSIONS))
                    .flatMap(Optional::stream)
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot list dashboard reports", exception);
        }
    }

    private Optional<DashboardDocumentSummary> summary(
            Path path, String kind, Set<String> extensions) {
        try {
            if (!Files.isRegularFile(path) || !extensions.contains(extension(path))) {
                return Optional.empty();
            }
            long size = Files.size(path);
            if (size > maximumBytes) {
                return Optional.empty();
            }
            Instant modified = Files.getLastModifiedTime(path).toInstant();
            return Optional.of(new DashboardDocumentSummary(
                    id(kind, path.getFileName().toString()), kind,
                    path.getFileName().toString(), size, modified));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private Optional<DashboardDocument> read(DashboardDocumentSummary summary) {
        Path path = REPORT.equals(summary.kind())
                ? reportDirectory.resolve(summary.name()).normalize()
                : rulesFile;
        if (!safePath(summary, path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new DashboardDocument(
                    summary.name(), mediaType(path), Files.readString(path, StandardCharsets.UTF_8)));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private boolean safePath(DashboardDocumentSummary summary, Path path) {
        if (REPORT.equals(summary.kind()) && !path.getParent().equals(reportDirectory)) {
            return false;
        }
        try {
            return Files.isRegularFile(path) && Files.size(path) <= maximumBytes;
        } catch (IOException exception) {
            return false;
        }
    }

    private static String id(String kind, String name) {
        String value = kind + ':' + name;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int separator = name.lastIndexOf('.');
        return separator < 0 ? "" : name.substring(separator + 1).toLowerCase();
    }

    private static String mediaType(Path path) {
        return switch (extension(path)) {
            case "json" -> "application/json";
            case "csv" -> "text/csv";
            case "html" -> "text/html";
            default -> "application/yaml";
        };
    }
}
