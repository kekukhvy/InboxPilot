package dev.inboxpilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Composition root and entry point for InboxPilot.
 *
 * <p>Starts the Spring context that wires the application together. Domain logic
 * lives in {@code dev.inboxpilot.domain} and stays free of framework types; this
 * class is the only place that knows how the application is bootstrapped.
 */
@SpringBootApplication
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
