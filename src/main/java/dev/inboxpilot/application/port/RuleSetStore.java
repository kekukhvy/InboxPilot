package dev.inboxpilot.application.port;

import dev.inboxpilot.domain.rules.RuleSet;
import java.nio.file.Path;

/** Loads one explicitly selected validated YAML rule set. */
public interface RuleSetStore {

    RuleSet load(Path path);
}
