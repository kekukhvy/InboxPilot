package dev.inboxpilot.infrastructure.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.ai.AcceptedAiRuleConverter;
import dev.inboxpilot.domain.ai.AiClassificationSuggestion;
import dev.inboxpilot.domain.ai.AiHumanDecision;
import dev.inboxpilot.domain.ai.ReviewedAiSuggestion;
import dev.inboxpilot.domain.rules.RuleSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("FileAcceptedRuleStore")
class FileAcceptedRuleStoreTest {

    @TempDir
    Path directory;

    @Test
    @DisplayName("writes YAML that the rule parser can load again")
    void persistsParseableYaml() throws Exception {
        ReviewedAiSuggestion reviewed = new ReviewedAiSuggestion(
                new AiClassificationSuggestion("example.com", "Label_News", 0.9, "Rationale"),
                AiHumanDecision.ACCEPT);
        RuleSet expected = new RuleSet(1, List.of(new AcceptedAiRuleConverter().convert(reviewed)));
        Path file = directory.resolve("accepted.yaml");

        new FileAcceptedRuleStore(file).save(expected);

        RuleSet actual = new RuleYamlParser().parse(Files.readString(file));
        assertThat(actual).isEqualTo(expected);
    }
}
