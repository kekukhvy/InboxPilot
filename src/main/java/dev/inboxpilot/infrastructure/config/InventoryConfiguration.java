package dev.inboxpilot.infrastructure.config;

import dev.inboxpilot.application.inventory.InventoryService;
import dev.inboxpilot.application.port.InventoryReportStore;
import dev.inboxpilot.application.port.MessageSnapshotStore;
import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.application.port.CheckpointStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the provider-neutral inventory use case to configured adapters. */
@Configuration(proxyBeanMethods = false)
public class InventoryConfiguration {

    @Bean
    InventoryService inventoryService(
            MessageSource source,
            InventoryReportStore reports,
            MessageSnapshotStore snapshots,
            CheckpointStore checkpoints,
            InboxPilotProperties properties) {
        ScanningProperties scanning = properties.scanning();
        return new InventoryService(
                source, reports, snapshots, checkpoints, properties.checkpoints().enabled(),
                scanning.lookback(), scanning.maxMessages(), Clock.systemUTC());
    }
}
