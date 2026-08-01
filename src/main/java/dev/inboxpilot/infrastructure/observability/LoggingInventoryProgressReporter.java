package dev.inboxpilot.infrastructure.observability;

import dev.inboxpilot.application.model.InventoryProgress;
import dev.inboxpilot.application.port.InventoryProgressReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Logs complete inventory progress snapshots for operators and CLI users. */
@Component
public class LoggingInventoryProgressReporter implements InventoryProgressReporter {

    private static final Logger logger =
            LoggerFactory.getLogger(LoggingInventoryProgressReporter.class);
    private static final String PROGRESS_TEMPLATE =
            "Inventory progress processed={} remaining={} retries={} throughput={}/s elapsed={} eta={}";
    private static final String UNKNOWN_ETA = "unknown";

    @Override
    public void report(InventoryProgress progress) {
        logger.info(PROGRESS_TEMPLATE,
                progress.processedMessages(),
                progress.remainingMessages(),
                progress.retryCount(),
                progress.messagesPerSecond(),
                progress.elapsed(),
                progress.eta().map(Object::toString).orElse(UNKNOWN_ETA));
    }
}
