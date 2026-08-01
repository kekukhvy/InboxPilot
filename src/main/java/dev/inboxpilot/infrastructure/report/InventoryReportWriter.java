package dev.inboxpilot.infrastructure.report;

import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.infrastructure.config.ReportFormat;
import dev.inboxpilot.infrastructure.config.ReportProperties;
import dev.inboxpilot.infrastructure.config.InboxPilotProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Writes deterministic inventory reports in configured machine-readable formats. */
@Component
public class InventoryReportWriter {

    private static final String CSV_FILE = "inventory.csv";
    private static final String JSON_FILE = "inventory.json";
    private static final String WRITE_FAILURE = "Inventory report could not be written: ";
    private static final String HTML_UNSUPPORTED = "HTML inventory export is not implemented";

    private final ReportProperties properties;
    private final InventoryCsvRenderer csvRenderer = new InventoryCsvRenderer();
    private final InventoryJsonRenderer jsonRenderer = new InventoryJsonRenderer();

    @Autowired
    public InventoryReportWriter(InboxPilotProperties properties) {
        this(properties.reports());
    }

    InventoryReportWriter(ReportProperties properties) {
        this.properties = properties;
    }

    public List<Path> write(Inventory inventory) {
        try {
            Files.createDirectories(properties.outputDirectory());
            List<Path> paths = new ArrayList<>();
            for (ReportFormat format : properties.formats()) {
                paths.add(writeFormat(inventory, format));
            }
            return List.copyOf(paths);
        } catch (IOException exception) {
            throw new ReportWriteException(WRITE_FAILURE + properties.outputDirectory(), exception);
        }
    }

    private Path writeFormat(Inventory inventory, ReportFormat format) throws IOException {
        return switch (format) {
            case CSV -> writeFile(CSV_FILE, csvRenderer.render(inventory));
            case JSON -> writeFile(JSON_FILE, jsonRenderer.render(inventory));
            case HTML -> throw new IllegalArgumentException(HTML_UNSUPPORTED);
        };
    }

    private Path writeFile(String fileName, String content) throws IOException {
        Path path = properties.outputDirectory().resolve(fileName);
        Files.writeString(path, content, StandardCharsets.UTF_8, openOptions());
        return path;
    }

    private OpenOption[] openOptions() {
        if (properties.overwrite()) {
            return new OpenOption[] {
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};
        }
        return new OpenOption[] {StandardOpenOption.CREATE_NEW};
    }
}
