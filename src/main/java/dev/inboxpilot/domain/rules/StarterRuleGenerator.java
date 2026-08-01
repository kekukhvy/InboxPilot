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
import java.util.Set;

/** Generates reviewable starter rules from unambiguous inventory label mappings. */
public final class StarterRuleGenerator {

    private static final int GENERATED_PRIORITY = 100;
    private static final int HASH_RADIX = 36;
    private static final String GENERATED_SUBJECT = "Generated inventory example";

    public RuleSet generate(
            Inventory inventory, Set<String> userLabelIds, int minimumMessageCount) {
        if (minimumMessageCount < 1) {
            throw new IllegalArgumentException("Minimum message count must be positive");
        }
        List<RuleDefinition> rules = new ArrayList<>();
        inventory.domains().stream()
                .map(domain -> domainRule(domain, userLabelIds, minimumMessageCount))
                .flatMap(Optional::stream)
                .forEach(rules::add);
        inventory.senders().stream()
                .map(sender -> senderRule(sender, userLabelIds, minimumMessageCount))
                .flatMap(Optional::stream)
                .forEach(rules::add);
        rules.sort(Comparator.comparing(rule -> rule.id().value()));
        return new RuleSet(RuleSet.CURRENT_VERSION, rules);
    }

    private static Optional<RuleDefinition> senderRule(
            SenderInventory sender, Set<String> labels, int minimumCount) {
        return mappedLabel(sender.statistics(), labels, minimumCount)
                .map(label -> rule(
                        "sender", "sender-exact", sender.sender().value(), label,
                        sender.sender(), subject(sender.statistics())));
    }

    private static Optional<RuleDefinition> domainRule(
            DomainInventory domain, Set<String> labels, int minimumCount) {
        return mappedLabel(domain.statistics(), labels, minimumCount)
                .map(label -> rule(
                        "domain", "sender-domain", domain.domain(), label,
                        new EmailAddress("example@" + domain.domain()), subject(domain.statistics())));
    }

    private static Optional<String> mappedLabel(
            InventoryStatistics statistics, Set<String> userLabelIds, int minimumCount) {
        if (statistics.messageCount() < minimumCount) {
            return Optional.empty();
        }
        List<String> matchingLabels = statistics.currentLabels().stream()
                .filter(userLabelIds::contains)
                .distinct()
                .toList();
        return matchingLabels.size() == 1 ? Optional.of(matchingLabels.getFirst()) : Optional.empty();
    }

    private static RuleDefinition rule(
            String prefix,
            String operator,
            String value,
            String label,
            EmailAddress exampleSender,
            String exampleSubject) {
        String idValue = prefix + "-" + slug(value) + "-" + Integer.toUnsignedString(value.hashCode(), HASH_RADIX);
        RuleConditionSpec condition = new RuleConditionSpec(
                operator, Map.of("value", value, "ignore-case", "true"), List.of());
        RuleActionSpec action = new RuleActionSpec("add-label", Map.of("label", label));
        RuleMetadata metadata = new RuleMetadata(
                "Generated from an unambiguous high-volume inventory mapping", "inventory", List.of("generated"));
        RuleTestCase test = new RuleTestCase("matches observed mapping", exampleSender, exampleSubject, true);
        return new RuleDefinition(new RuleId(idValue), GENERATED_PRIORITY, condition,
                List.of(action), RuleConflictBehavior.CONTINUE, metadata, List.of(test));
    }

    private static String subject(InventoryStatistics statistics) {
        return statistics.sampleSubjects().isEmpty()
                ? GENERATED_SUBJECT
                : statistics.sampleSubjects().getFirst();
    }

    private static String slug(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
