package dev.inboxpilot.infrastructure.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.inboxpilot.domain.rules.RuleConflictBehavior;
import dev.inboxpilot.domain.rules.RuleSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RuleYamlParser")
class RuleYamlParserTest {

    @Test
    @DisplayName("parses the canonical rule example")
    void parsesCanonicalExample() throws IOException {
        String yaml = Files.readString(Path.of("doc/rules.example.yaml"));

        RuleSet rules = new RuleYamlParser().parse(yaml);

        assertThat(rules.version()).isEqualTo(RuleSet.CURRENT_VERSION);
        assertThat(rules.rules()).singleElement().satisfies(rule -> {
            assertThat(rule.id().value()).isEqualTo("newsletter-example");
            assertThat(rule.conflictBehavior()).isEqualTo(RuleConflictBehavior.CONTINUE);
            assertThat(rule.condition().parameters()).containsEntry("value", "example.com");
        });
    }

    @Test
    @DisplayName("reports the exact path of a missing required field")
    void reportsMissingFieldPath() {
        String yaml = """
                version: 1
                rules:
                  - id: incomplete
                    priority: 1
                """;

        assertThatThrownBy(() -> new RuleYamlParser().parse(yaml))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("rules[0].match");
    }

    @Test
    @DisplayName("translates malformed YAML into an actionable validation error")
    void reportsMalformedYaml() {
        assertThatThrownBy(() -> new RuleYamlParser().parse("rules: [unterminated"))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("Invalid YAML");
    }
}
