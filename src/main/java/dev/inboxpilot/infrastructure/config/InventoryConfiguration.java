package dev.inboxpilot.infrastructure.config;

import dev.inboxpilot.application.inventory.InventoryService;
import dev.inboxpilot.application.port.InventoryReportStore;
import dev.inboxpilot.application.port.MessageSource;
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
            InboxPilotProperties properties) {
        ScanningProperties scanning = properties.scanning();
        return new InventoryService(
                source, reports, scanning.lookback(), scanning.maxMessages(), Clock.systemUTC());
    }
}
