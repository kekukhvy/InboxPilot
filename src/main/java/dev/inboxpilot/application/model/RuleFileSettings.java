package dev.inboxpilot.application.model;

import java.nio.file.Path;
import java.util.Objects;

/** Local paths controlling which rules are trusted by default. */
public record RuleFileSettings(Path approvedRulesFile) {

    public RuleFileSettings {
        Objects.requireNonNull(approvedRulesFile, "approvedRulesFile");
    }
}
