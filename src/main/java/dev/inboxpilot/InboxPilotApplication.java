package dev.inboxpilot;

import dev.inboxpilot.infrastructure.config.InboxPilotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Composition root and entry point for InboxPilot.
 *
 * <p>Starts the Spring context that wires the application together. Domain logic
 * lives in {@code dev.inboxpilot.domain} and stays free of framework types; this
 * class is the only place that knows how the application is bootstrapped.
 */
@SpringBootApplication
@EnableConfigurationProperties(InboxPilotProperties.class)
public class InboxPilotApplication {

    /**
     * Starts the application.
     *
     * @param args command-line arguments passed through to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(InboxPilotApplication.class, args);
    }
}
