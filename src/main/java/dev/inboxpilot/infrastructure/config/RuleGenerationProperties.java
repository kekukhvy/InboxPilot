package dev.inboxpilot.infrastructure.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.Map;
import java.util.Set;
import java.nio.file.Path;

/** Safety policy for deterministic starter-rule generation. */
public record RuleGenerationProperties(
        @NotNull Set<String> excludedDomains,
        @NotNull Map<String, String> knownDomainMappings,
        @NotNull Set<String> senderRuleAllowlist,
        @Min(1) int minimumMessageCount,
        @NotNull Path approvedRulesFile) {

    public RuleGenerationProperties {
        excludedDomains = Set.copyOf(excludedDomains);
        knownDomainMappings = Map.copyOf(knownDomainMappings);
        senderRuleAllowlist = Set.copyOf(senderRuleAllowlist);
    }
}
