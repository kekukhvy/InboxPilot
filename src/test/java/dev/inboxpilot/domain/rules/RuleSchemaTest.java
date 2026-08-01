package dev.inboxpilot.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("YAML rule schema domain model")
class RuleSchemaTest {

    @Test
    @DisplayName("captures identity, priority, matching, actions, conflict behavior, and metadata")
    void capturesCompleteRuleContract() {
        RuleDefinition rule = new RuleDefinition(
                new RuleId("newsletter"),
                100,
                new RuleConditionSpec("sender-domain", Map.of("value", "example.com"), List.of()),
                List.of(new RuleActionSpec("add-label", Map.of("label", "Newsletters"))),
                RuleConflictBehavior.CONTINUE,
                new RuleMetadata("Newsletter classification", "generated", List.of("bulk")));

        assertThat(rule.id().value()).isEqualTo("newsletter");
        assertThat(rule.priority()).isEqualTo(100);
        assertThat(rule.condition().operator()).isEqualTo("sender-domain");
        assertThat(rule.actions()).singleElement().extracting(RuleActionSpec::type).isEqualTo("add-label");
        assertThat(rule.metadata().tags()).containsExactly("bulk");
    }

    @Test
    @DisplayName("keeps nested schema collections immutable")
    void keepsCollectionsImmutable() {
        List<String> tags = new ArrayList<>(List.of("bulk"));
        RuleMetadata metadata = new RuleMetadata("Description", "manual", tags);
        tags.add("changed");

        assertThat(metadata.tags()).containsExactly("bulk");
        assertThatThrownBy(() -> metadata.tags().add("later"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("rejects invalid identity, priority, and empty actions")
    void rejectsInvalidRule() {
        RuleConditionSpec condition = new RuleConditionSpec("sender", Map.of("value", "a@b.com"), List.of());
        RuleMetadata metadata = new RuleMetadata("Description", "manual", List.of());

        assertThatThrownBy(() -> new RuleId("Bad ID"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RuleDefinition(
                        new RuleId("valid"), -1, condition, List.of(), RuleConflictBehavior.CONTINUE, metadata))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RuleDefinition(
                        new RuleId("valid"), 1, condition, List.of(), RuleConflictBehavior.CONTINUE, metadata))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
