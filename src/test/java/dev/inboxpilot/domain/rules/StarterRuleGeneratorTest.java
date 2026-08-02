package dev.inboxpilot.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.inventory.DomainInventory;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.inventory.SenderInventory;
import dev.inboxpilot.domain.message.EmailAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StarterRuleGenerator")
class StarterRuleGeneratorTest {

    private static final String NEWSLETTER = "Label_News";
    private static final String OTHER = "Label_Other";
    private static final String NEWSLETTER_NAME = "Newsletters";
    private static final String OTHER_NAME = "Other";
    private static final String PRIVATE_RELAY = "privaterelay.appleid.com";
    private static final String VIGNETTE_DOMAIN = "e-autopalyamatrica.hu";
    private static final String TOLLS = "Mobility/Tolls";
    private static final int MINIMUM_MESSAGES = 20;

    @Test
    @DisplayName("generates deterministic rules only for high-confidence mappings")
    void generatesHighConfidenceRules() {
        Inventory inventory = new Inventory(
                List.of(
                        new SenderInventory(new EmailAddress("digest@example.com"), stats(30, NEWSLETTER)),
                        new SenderInventory(new EmailAddress("mixed@example.org"), stats(40, NEWSLETTER, OTHER))),
                List.of(new DomainInventory("vendor.example", stats(50, NEWSLETTER))));

        RuleSet rules = new StarterRuleGenerator()
                .generate(inventory, Map.of(
                        NEWSLETTER, NEWSLETTER_NAME, OTHER, OTHER_NAME),
                        new RuleGenerationPolicy(Set.of(), Map.of(), Set.of(), 20))
                .rules();

        assertThat(rules.rules()).hasSize(2);
        assertThat(rules.rules()).extracting(rule -> rule.condition().operator())
                .containsExactly("sender-exact", "sender-domain");
        assertThat(rules.rules()).allSatisfy(rule -> {
            assertThat(rule.actions().getFirst().parameters())
                    .containsEntry("label", NEWSLETTER_NAME);
            assertThat(rule.metadata().source()).isEqualTo("inventory");
            assertThat(rule.tests()).hasSize(1);
        });
    }

    @Test
    void rejectsExcludedDomainButKeepsExactSenderSuggestion() {
        Inventory inventory = inventoryFor(
                PRIVATE_RELAY, "relay@" + PRIVATE_RELAY, NEWSLETTER_NAME);

        RuleGenerationResult result = generator(inventory, policy(
                Set.of(PRIVATE_RELAY), Map.of(), Set.of()));

        assertThat(result.rules().rules()).singleElement().satisfies(rule ->
                assertThat(rule.condition().operator()).isEqualTo("sender-exact"));
        assertThat(result.rejectedSuggestions()).singleElement().satisfies(rejected -> {
            assertThat(rejected.domain()).isEqualTo(PRIVATE_RELAY);
            assertThat(rejected.reason()).isEqualTo("excluded-domain");
        });
    }

    @Test
    void appliesKnownLogicalLabelAndDeduplicatesEquivalentSenderRule() {
        Inventory inventory = inventoryFor(
                VIGNETTE_DOMAIN, "noreply@" + VIGNETTE_DOMAIN, NEWSLETTER_NAME);

        RuleGenerationResult result = generator(inventory, policy(
                Set.of(), Map.of(VIGNETTE_DOMAIN, TOLLS), Set.of()));

        assertThat(result.rules().rules()).singleElement().satisfies(rule -> {
            assertThat(rule.condition().operator()).isEqualTo("sender-domain");
            assertThat(rule.actions().getFirst().parameters()).containsEntry("label", TOLLS);
        });
        assertThat(result.deduplicatedSuggestions()).isEqualTo(1);
    }

    @Test
    void keepsSenderRuleWhenItAssignsADifferentLabel() {
        Inventory inventory = new Inventory(
                List.of(new SenderInventory(
                        new EmailAddress("person@example.com"), stats(30, OTHER))),
                List.of(new DomainInventory("example.com", stats(30, NEWSLETTER))));

        RuleGenerationResult result = generator(inventory, policy(
                Set.of(), Map.of(), Set.of()));

        assertThat(result.rules().rules()).hasSize(2);
        assertThat(result.deduplicatedSuggestions()).isZero();
    }

    @Test
    void producesTheSameRulesForTheSameInventory() {
        Inventory inventory = inventoryFor(
                VIGNETTE_DOMAIN, "noreply@" + VIGNETTE_DOMAIN, NEWSLETTER_NAME);
        RuleGenerationPolicy policy = policy(
                Set.of(), Map.of(VIGNETTE_DOMAIN, TOLLS), Set.of());

        RuleGenerationResult first = generator(inventory, policy);
        RuleGenerationResult second = generator(inventory, policy);

        assertThat(second).isEqualTo(first);
    }

    private static RuleGenerationResult generator(
            Inventory inventory, RuleGenerationPolicy policy) {
        return new StarterRuleGenerator().generate(inventory, Map.of(
                NEWSLETTER, NEWSLETTER_NAME, OTHER, OTHER_NAME), policy);
    }

    private static RuleGenerationPolicy policy(
            Set<String> excluded, Map<String, String> known, Set<String> allowlist) {
        return new RuleGenerationPolicy(excluded, known, allowlist, MINIMUM_MESSAGES);
    }

    private static Inventory inventoryFor(String domain, String sender, String labelName) {
        String labelId = NEWSLETTER_NAME.equals(labelName) ? NEWSLETTER : OTHER;
        return new Inventory(
                List.of(new SenderInventory(new EmailAddress(sender), stats(30, labelId))),
                List.of(new DomainInventory(domain, stats(30, labelId))));
    }

    private static InventoryStatistics stats(int messages, String... labels) {
        Instant receivedAt = Instant.parse("2026-01-01T00:00:00Z");
        return new InventoryStatistics(
                messages, 0, receivedAt, receivedAt, List.of(labels), List.of("Digest"));
    }
}
