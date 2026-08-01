package dev.inboxpilot.infrastructure.dashboard;

import dev.inboxpilot.application.dashboard.DashboardQueryService;
import dev.inboxpilot.application.port.DashboardDocumentStore;
import dev.inboxpilot.infrastructure.config.DashboardProperties;
import dev.inboxpilot.infrastructure.config.ReportProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires dashboard queries only when the optional feature is enabled. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "inboxpilot.dashboard", name = "enabled", havingValue = "true")
public class DashboardConfiguration {

    @Bean
    DashboardDocumentStore dashboardDocumentStore(
            ReportProperties reports, DashboardProperties dashboard) {
        return new FileDashboardDocumentStore(reports, dashboard);
    }

    @Bean
    DashboardQueryService dashboardQueryService(DashboardDocumentStore store) {
        return new DashboardQueryService(store);
    }
}
