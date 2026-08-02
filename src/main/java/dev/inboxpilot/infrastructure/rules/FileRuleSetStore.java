package dev.inboxpilot.infrastructure.rules;

import dev.inboxpilot.application.port.RuleSetStore;
import dev.inboxpilot.domain.rules.RuleSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/** Loads rule YAML from an explicit local path through the safe parser. */
@Component
public final class FileRuleSetStore implements RuleSetStore {

    private static final String READ_FAILURE = "Rule file could not be read: ";
    private final RuleYamlParser parser = new RuleYamlParser();

    @Override
    public RuleSet load(Path path) {
        try {
            return parser.parse(Files.readString(path));
        } catch (IOException exception) {
            throw new RuleValidationException(READ_FAILURE + path, exception);
        }
    }
}
