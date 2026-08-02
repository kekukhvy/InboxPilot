package dev.inboxpilot.infrastructure.config;

import dev.inboxpilot.application.classification.ClassificationDryRunService;
import dev.inboxpilot.application.model.RuleFileSettings;
import dev.inboxpilot.application.port.ClassificationDryRunReportStore;
import dev.inboxpilot.application.port.InventorySnapshotStore;
import dev.inboxpilot.application.port.MailboxGateway;
import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.application.port.RuleSetStore;
import dev.inboxpilot.domain.rules.RuleGenerationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires classification simulations without any mutation gateway. */
@Configuration(proxyBeanMethods = false)
public class ClassificationConfiguration {

    @Bean
    RuleFileSettings ruleFileSettings(InboxPilotProperties properties) {
        return new RuleFileSettings(properties.ruleGeneration().approvedRulesFile());
    }

    @Bean
    ClassificationDryRunService classificationDryRunService(
            InventorySnapshotStore inventoryStore,
            RuleSetStore ruleStore,
            MessageSource messageSource,
            MailboxGateway mailboxGateway,
            ClassificationDryRunReportStore reportStore,
            RuleGenerationPolicy rulePolicy) {
        return new ClassificationDryRunService(
                inventoryStore, ruleStore, messageSource, mailboxGateway, reportStore, rulePolicy);
    }
}
