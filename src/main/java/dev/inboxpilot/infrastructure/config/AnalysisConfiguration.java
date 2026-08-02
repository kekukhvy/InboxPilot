package dev.inboxpilot.infrastructure.config;

import dev.inboxpilot.application.analysis.AnalysisService;
import dev.inboxpilot.application.port.InventorySnapshotStore;
import dev.inboxpilot.application.port.AnalysisReportStore;
import dev.inboxpilot.application.port.MailboxGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires read-only mailbox analysis. */
@Configuration(proxyBeanMethods = false)
public class AnalysisConfiguration {

    @Bean
    AnalysisService analysisService(
            InventorySnapshotStore inventoryStore,
            AnalysisReportStore reportStore,
            MailboxGateway mailboxGateway) {
        return new AnalysisService(inventoryStore, reportStore, mailboxGateway);
    }
}
