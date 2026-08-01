package dev.inboxpilot.domain.analysis;

import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Detects labels that are empty in the scan and labels with duplicate names. */
public final class LabelStructureAnalyzer {

    private static final int MINIMUM_DUPLICATE_COUNT = 2;
    private static final Comparator<LabelDefinition> LABEL_ORDER =
            Comparator.comparing(LabelStructureAnalyzer::normalizedName)
                    .thenComparing(LabelDefinition::id);

    public LabelStructureAnalysis analyze(Collection<LabelDefinition> labels, Inventory inventory) {
        Set<String> usedLabelIds = inventory.senders().stream()
                .map(sender -> sender.statistics())
                .map(InventoryStatistics::currentLabels)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        List<LabelDefinition> unusedLabels = labels.stream()
                .filter(label -> !usedLabelIds.contains(label.id()))
                .sorted(LABEL_ORDER)
                .toList();
        return new LabelStructureAnalysis(unusedLabels, duplicateGroups(labels));
    }

    private static List<List<LabelDefinition>> duplicateGroups(Collection<LabelDefinition> labels) {
        Map<String, List<LabelDefinition>> labelsByName = labels.stream().collect(Collectors.groupingBy(
                LabelStructureAnalyzer::normalizedName, LinkedHashMap::new, Collectors.toList()));
        return labelsByName.values().stream()
                .filter(group -> group.size() >= MINIMUM_DUPLICATE_COUNT)
                .map(group -> group.stream().sorted(LABEL_ORDER).toList())
                .sorted(Comparator.comparing(group -> normalizedName(group.get(0))))
                .toList();
    }

    private static String normalizedName(LabelDefinition label) {
        return label.name().strip().toLowerCase(Locale.ROOT);
    }
}
