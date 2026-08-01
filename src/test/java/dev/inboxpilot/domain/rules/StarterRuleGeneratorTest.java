package dev.inboxpilot.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.inventory.DomainInventory;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.inventory.SenderInventory;
import dev.inboxpilot.domain.message.EmailAddress;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StarterRuleGenerator")
class StarterRuleGeneratorTest {

    private static final String NEWSLETTER = "Label_News";
    private static final String OTHER = "Label_Other";

    @Test
    @DisplayName("generates deterministic rules only for high-confidence mappings")
    void generatesHighConfidenceRules() {
        Inventory inventory = new Inventory(
                List.of(
                        new SenderInventory(new EmailAddress("digest@example.com"), stats(30, NEWSLETTER)),
                        new SenderInventory(new EmailAddress("mixed@example.org"), stats(40, NEWSLETTER, OTHER))),
                List.of(new DomainInventory("vendor.example", stats(50, NEWSLETTER))));

        RuleSet rules = new StarterRuleGenerator()
                .generate(inventory, Set.of(NEWSLETTER, OTHER), 20);

        assertThat(rules.rules()).hasSize(2);
        assertThat(rules.rules()).extracting(rule -> rule.condition().operator())
                .containsExactly("sender-domain", "sender-exact");
        assertThat(rules.rules()).allSatisfy(rule -> {
            assertThat(rule.actions().getFirst().parameters()).containsEntry("label", NEWSLETTER);
            assertThat(rule.metadata().source()).isEqualTo("inventory");
            assertThat(rule.tests()).hasSize(1);
        });
    }

    private static InventoryStatistics stats(int messages, String... labels) {
        Instant receivedAt = Instant.parse("2026-01-01T00:00:00Z");
        return new InventoryStatistics(
                messages, 0, receivedAt, receivedAt, List.of(labels), List.of("Digest"));
    }
}
