package dev.inboxpilot.infrastructure.config;

import dev.inboxpilot.application.analysis.AnalysisService;
import dev.inboxpilot.application.port.MailboxGateway;
import dev.inboxpilot.application.port.MessageSource;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires read-only mailbox analysis. */
@Configuration(proxyBeanMethods = false)
public class AnalysisConfiguration {

    @Bean
    AnalysisService analysisService(
            MessageSource source,
            MailboxGateway gateway,
            InboxPilotProperties properties) {
        ScanningProperties scanning = properties.scanning();
        return new AnalysisService(
                source, gateway, scanning.lookback(), scanning.maxMessages(), Clock.systemUTC());
    }
}
