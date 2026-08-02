package dev.inboxpilot.infrastructure.config;

import dev.inboxpilot.application.analysis.AnalysisService;
import dev.inboxpilot.application.port.InventorySnapshotStore;
import dev.inboxpilot.application.port.AnalysisReportStore;
import dev.inboxpilot.application.port.MailboxGateway;
import dev.inboxpilot.domain.rules.RuleGenerationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires read-only mailbox analysis. */
@Configuration(proxyBeanMethods = false)
public class AnalysisConfiguration {

    @Bean
    RuleGenerationPolicy ruleGenerationPolicy(InboxPilotProperties properties) {
        RuleGenerationProperties generation = properties.ruleGeneration();
        return new RuleGenerationPolicy(
                generation.excludedDomains(), generation.knownDomainMappings(),
                generation.senderRuleAllowlist(), generation.minimumMessageCount());
    }

    @Bean
    AnalysisService analysisService(
            InventorySnapshotStore inventoryStore,
            AnalysisReportStore reportStore,
            MailboxGateway mailboxGateway,
            RuleGenerationPolicy policy) {
        return new AnalysisService(inventoryStore, reportStore, mailboxGateway, policy);
    }
}
