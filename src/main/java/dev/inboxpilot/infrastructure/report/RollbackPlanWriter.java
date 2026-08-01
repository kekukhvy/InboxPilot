package dev.inboxpilot.infrastructure.report;

import dev.inboxpilot.domain.classification.RollbackPlan;
import dev.inboxpilot.application.port.RollbackPlanStore;
import dev.inboxpilot.infrastructure.config.InboxPilotProperties;
import dev.inboxpilot.infrastructure.config.ReportProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Persists the rollback artifact that must exist before label execution starts. */
@Component
public final class RollbackPlanWriter implements RollbackPlanStore {

    private static final String FILE_NAME = "classification-rollback.json";
    private static final String FAILURE_MESSAGE = "Rollback plan could not be written: ";
    private final ReportProperties properties;
    private final RollbackPlanJsonRenderer renderer = new RollbackPlanJsonRenderer();

    @Autowired
    public RollbackPlanWriter(InboxPilotProperties properties) {
        this(properties.reports());
    }

    RollbackPlanWriter(ReportProperties properties) {
        this.properties = properties;
    }

    public Path write(RollbackPlan plan) {
        Path path = properties.outputDirectory().resolve(FILE_NAME);
        try {
            Files.createDirectories(properties.outputDirectory());
            Files.writeString(path, renderer.render(plan), StandardCharsets.UTF_8, openOptions());
            return path;
        } catch (IOException exception) {
            throw new ReportWriteException(FAILURE_MESSAGE + path, exception);
        }
    }

    @Override
    public void save(RollbackPlan plan) {
        write(plan);
    }

    private OpenOption[] openOptions() {
        if (properties.overwrite()) {
            return new OpenOption[] {
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};
        }
        return new OpenOption[] {StandardOpenOption.CREATE_NEW};
    }
}
