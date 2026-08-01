package dev.inboxpilot.infrastructure.gmail;

import java.time.Duration;

@FunctionalInterface
interface RetrySleeper {

    void sleep(Duration duration) throws InterruptedException;
}
