package dev.inboxpilot.domain.rules;

import dev.inboxpilot.domain.inventory.DomainInventory;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.inventory.SenderInventory;
import dev.inboxpilot.domain.message.EmailAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Generates safe name-based starter rules from inventory label mappings. */
public final class StarterRuleGenerator {

    private static final int GENERATED_PRIORITY = 100;
    private static final int HASH_RADIX = 36;
    private static final String GENERATED_SUBJECT = "Generated inventory example";
    private static final String EXCLUDED_DOMAIN_REASON = "excluded-domain";
    private final RuleSuggestionDeduplicator deduplicator = new RuleSuggestionDeduplicator();

    public RuleGenerationResult generate(
            Inventory inventory,
            Map<String, String> userLabelNamesById,
            RuleGenerationPolicy policy) {
        List<RejectedRuleSuggestion> rejected = new ArrayList<>();
        List<RuleDefinition> domainRules = domainRules(
                inventory, userLabelNamesById, policy, rejected);
        List<RuleDefinition> senderRules = senderRules(
                inventory, userLabelNamesById, policy);
        int generated = domainRules.size() + senderRules.size() + rejected.size();
        List<RuleDefinition> deduplicatedSenders = deduplicator.deduplicate(
                senderRules, domainRules, policy.senderRuleAllowlist());
        int deduplicated = senderRules.size() - deduplicatedSenders.size();
        List<RuleDefinition> rules = new ArrayList<>(domainRules);
        rules.addAll(deduplicatedSenders);
        rules.sort(RuleSpecificity.order());
        return new RuleGenerationResult(
                new RuleSet(RuleSet.CURRENT_VERSION, rules), rejected,
                generated, deduplicated);
    }

    private static List<RuleDefinition> domainRules(
            Inventory inventory,
            Map<String, String> labels,
            RuleGenerationPolicy policy,
            List<RejectedRuleSuggestion> rejected) {
        List<RuleDefinition> rules = new ArrayList<>();
        for (DomainInventory domain : inventory.domains()) {
            Optional<String> label = mappedLabelName(
                    domain.domain(), domain.statistics(), labels, policy);
            if (label.isEmpty()) {
                continue;
            }
            if (policy.excludedDomains().contains(domain.domain())) {
                rejected.add(new RejectedRuleSuggestion(
                        "", domain.domain(), domain.statistics().messageCount(),
                        EXCLUDED_DOMAIN_REASON));
                continue;
            }
            rules.add(domainRule(domain, label.orElseThrow()));
        }
        return rules;
    }

    private static List<RuleDefinition> senderRules(
            Inventory inventory,
            Map<String, String> labels,
            RuleGenerationPolicy policy) {
        return inventory.senders().stream()
                .map(sender -> mappedLabelName(
                        sender.sender().domain(), sender.statistics(), labels, policy)
                        .map(label -> senderRule(sender, label)))
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<String> mappedLabelName(
            String domain,
            InventoryStatistics statistics,
            Map<String, String> labels,
            RuleGenerationPolicy policy) {
        if (statistics.messageCount() < policy.minimumMessageCount()) {
            return Optional.empty();
        }
        String known = policy.knownDomainMappings().get(domain);
        if (known != null) {
            return Optional.of(known);
        }
        List<String> matching = statistics.currentLabels().stream()
                .filter(labels::containsKey)
                .distinct()
                .toList();
        return matching.size() == 1
                ? Optional.of(labels.get(matching.getFirst()))
                : Optional.empty();
    }

    private static RuleDefinition senderRule(SenderInventory sender, String label) {
        return rule("sender", "sender-exact", sender.sender().value(), label,
                sender.sender(), subject(sender.statistics()));
    }

    private static RuleDefinition domainRule(DomainInventory domain, String label) {
        return rule("domain", "sender-domain", domain.domain(), label,
                new EmailAddress("example@" + domain.domain()), subject(domain.statistics()));
    }

    private static RuleDefinition rule(
            String prefix,
            String operator,
            String value,
            String label,
            EmailAddress exampleSender,
            String exampleSubject) {
        String idValue = prefix + "-" + slug(value) + "-"
                + Integer.toUnsignedString(value.hashCode(), HASH_RADIX);
        RuleConditionSpec condition = new RuleConditionSpec(
                operator, Map.of("value", value, "ignore-case", "true"), List.of());
        RuleActionSpec action = new RuleActionSpec("add-label", Map.of("label", label));
        RuleMetadata metadata = new RuleMetadata(
                "Generated from an unambiguous high-volume inventory mapping",
                "inventory", List.of("generated"));
        RuleTestCase test = new RuleTestCase(
                "matches observed mapping", exampleSender, exampleSubject, true);
        return new RuleDefinition(new RuleId(idValue), GENERATED_PRIORITY, condition,
                List.of(action), RuleConflictBehavior.CONTINUE, metadata, List.of(test));
    }

    private static String subject(InventoryStatistics statistics) {
        return statistics.sampleSubjects().isEmpty()
                ? GENERATED_SUBJECT : statistics.sampleSubjects().getFirst();
    }

    private static String slug(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
