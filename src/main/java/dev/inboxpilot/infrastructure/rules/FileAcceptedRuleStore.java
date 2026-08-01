package dev.inboxpilot.infrastructure.rules;

import dev.inboxpilot.application.port.AcceptedRuleStore;
import dev.inboxpilot.domain.rules.RuleSet;
import dev.inboxpilot.infrastructure.config.InboxPilotProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Persists the complete accepted-AI rule set as a local YAML artifact. */
@Component
public final class FileAcceptedRuleStore implements AcceptedRuleStore {

    private static final String FILE_NAME = "accepted-ai-rules.yaml";
    private final Path file;
    private final RuleYamlRenderer renderer = new RuleYamlRenderer();

    @Autowired
    public FileAcceptedRuleStore(InboxPilotProperties properties) {
        file = properties.reports().outputDirectory().resolve(FILE_NAME);
    }

    FileAcceptedRuleStore(Path file) {
        this.file = file;
    }

    @Override
    public void save(RuleSet ruleSet) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, renderer.render(ruleSet), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new RuleValidationException("Accepted AI rules could not be persisted: " + file, exception);
        }
    }
}
