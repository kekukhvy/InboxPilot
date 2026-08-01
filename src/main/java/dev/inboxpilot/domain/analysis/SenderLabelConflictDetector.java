package dev.inboxpilot.domain.analysis;

import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.SenderInventory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Detects sender labels that coexist despite belonging to an incompatible group. */
public final class SenderLabelConflictDetector {

    private static final int MINIMUM_CONFLICTING_LABELS = 2;

    public List<SenderLabelConflict> detect(
            Inventory inventory,
            List<Set<String>> incompatibleLabelGroups) {
        List<SenderLabelConflict> conflicts = new ArrayList<>();
        for (SenderInventory sender : inventory.senders()) {
            detectForSender(sender, incompatibleLabelGroups).forEach(conflicts::add);
        }
        return conflicts.stream()
                .sorted(Comparator.comparing(conflict -> conflict.sender().value()))
                .toList();
    }

    private static List<SenderLabelConflict> detectForSender(
            SenderInventory sender,
            List<Set<String>> groups) {
        Set<String> currentLabels = new HashSet<>(sender.statistics().currentLabels());
        return groups.stream()
                .map(group -> intersection(currentLabels, group))
                .filter(labels -> labels.size() >= MINIMUM_CONFLICTING_LABELS)
                .map(labels -> new SenderLabelConflict(
                        sender.sender(), labels, sender.statistics().sampleSubjects()))
                .toList();
    }

    private static List<String> intersection(Set<String> current, Set<String> group) {
        return group.stream().filter(current::contains).sorted().toList();
    }
}
