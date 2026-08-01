package dev.inboxpilot.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.inboxpilot.domain.error.Failure;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Covers AC2 of issue #5 — a log line carries the operation, the message id when
 * one applies, the retry attempt, and how long the operation took.
 *
 * <p>Asserts against the {@link MDC} directly rather than against rendered log
 * output: the MDC is the contract {@code OperationContext} owns, while the
 * rendering of it is a Logback pattern in {@code application.yml}.
 */
@DisplayName("OperationContext")
class OperationContextTest {

    private static final String OPERATION = "gmail.fetch";
    private static final String OUTER_OPERATION = "scan.run";
    private static final String MESSAGE_ID = "18f2a1c9d0";
    private static final String OUTER_MESSAGE_ID = "0000000000";

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    @AfterEach
    void clearDiagnosticContext() {
        // The MDC is thread-local and JUnit reuses threads, so a leak here would
        // surface as a mystery failure in an unrelated test.
        MDC.clear();
    }

    @AfterEach
    void detachAppender() {
        if (appender != null) {
            contextLogger().detachAppender(appender);
            appender = null;
        }
    }

    /** Captures what {@link OperationContext} logs, so assertions see real events. */
    private ListAppender<ILoggingEvent> attachAppender() {
        appender = new ListAppender<>();
        appender.start();
        contextLogger().addAppender(appender);
        return appender;
    }

    private static ch.qos.logback.classic.Logger contextLogger() {
        return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(OperationContext.class);
    }

    @Test
    @DisplayName("records the operation name for every line logged inside it")
    void recordsOperationName() {
        try (OperationContext operation = OperationContext.start(OPERATION)) {
            assertThat(MDC.get(OperationContext.OPERATION_KEY)).isEqualTo(OPERATION);
        }
    }

    @Test
    @DisplayName("records the message id, the attempt, and the elapsed time together")
    void recordsTheFullOperationalContext() {
        try (OperationContext operation = OperationContext.start(OPERATION)) {
            operation.withMessageId(MESSAGE_ID).withAttempt(2);

            assertThat(MDC.get(OperationContext.OPERATION_KEY)).isEqualTo(OPERATION);
            assertThat(MDC.get(OperationContext.MESSAGE_ID_KEY)).isEqualTo(MESSAGE_ID);
            assertThat(MDC.get(OperationContext.ATTEMPT_KEY)).isEqualTo("2");

            operation.succeeded();
            assertThat(MDC.get(OperationContext.ELAPSED_MILLIS_KEY)).isNotNull();
        }
    }

    @Test
    @DisplayName("leaves the message id unset for operations that process no single message")
    void omitsMessageIdWhenNoneApplies() {
        try (OperationContext operation = OperationContext.start(OPERATION)) {
            operation.withMessageId(null);

            assertThat(MDC.get(OperationContext.MESSAGE_ID_KEY)).isNull();
        }
    }

    @Test
    @DisplayName("reports elapsed time that advances while the operation runs")
    void reportsElapsedTime() throws InterruptedException {
        try (OperationContext operation = OperationContext.start(OPERATION)) {
            Thread.sleep(5);

            assertThat(operation.elapsed()).isPositive();
        }
    }

    @Nested
    @DisplayName("on completion")
    class OnCompletion {

        @Test
        @DisplayName("records success with the elapsed time and no failure class")
        void recordsSuccess() {
            try (OperationContext operation = OperationContext.start(OPERATION)) {
                operation.succeeded();

                assertThat(MDC.get(OperationContext.OUTCOME_KEY)).isEqualTo("success");
                assertThat(MDC.get(OperationContext.ELAPSED_MILLIS_KEY)).isNotNull();
                assertThat(MDC.get(OperationContext.FAILURE_KEY)).isNull();
            }
        }

        @Test
        @DisplayName("records a transient failure so the retry is visible in the log")
        void recordsTransientFailure() {
            try (OperationContext operation = OperationContext.start(OPERATION)) {
                operation.failed(Failure.TRANSIENT);

                assertThat(MDC.get(OperationContext.OUTCOME_KEY)).isEqualTo("failure");
                assertThat(MDC.get(OperationContext.FAILURE_KEY)).isEqualTo("TRANSIENT");
            }
        }

        @Test
        @DisplayName("records a permanent failure distinctly from a transient one")
        void recordsPermanentFailure() {
            try (OperationContext operation = OperationContext.start(OPERATION)) {
                operation.failed(Failure.PERMANENT);

                assertThat(MDC.get(OperationContext.FAILURE_KEY)).isEqualTo("PERMANENT");
            }
        }

        @Test
        @DisplayName("still reports failure when the classification is unknown")
        void recordsUnclassifiedFailure() {
            try (OperationContext operation = OperationContext.start(OPERATION)) {
                operation.failed(null);

                assertThat(MDC.get(OperationContext.OUTCOME_KEY)).isEqualTo("failure");
                assertThat(MDC.get(OperationContext.FAILURE_KEY)).isNull();
            }
        }

        @Test
        @DisplayName("warns, with the context attached, when no outcome was recorded")
        void warnsWhenNoOutcomeWasRecorded() {
            ListAppender<ILoggingEvent> events = attachAppender();

            // An operation that threw past succeeded()/failed() has to be logged
            // as it closes: afterwards the context that would explain it is gone.
            try (OperationContext operation = OperationContext.start(OPERATION)) {
                operation.withMessageId(MESSAGE_ID);
            }

            assertThat(events.list).singleElement().satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getMDCPropertyMap())
                        .containsEntry(OperationContext.OPERATION_KEY, OPERATION)
                        .containsEntry(OperationContext.MESSAGE_ID_KEY, MESSAGE_ID)
                        .containsEntry(OperationContext.OUTCOME_KEY, "unknown")
                        .containsKey(OperationContext.ELAPSED_MILLIS_KEY);
            });
        }

        @Test
        @DisplayName("stays quiet when the operation did record an outcome")
        void doesNotWarnWhenAnOutcomeWasRecorded() {
            ListAppender<ILoggingEvent> events = attachAppender();

            try (OperationContext operation = OperationContext.start(OPERATION)) {
                operation.succeeded();
            }

            assertThat(events.list).isEmpty();
        }
    }

    @Nested
    @DisplayName("when nested inside another operation")
    class WhenNested {

        @Test
        @DisplayName("restores the enclosing operation's context on close")
        void restoresEnclosingContext() {
            try (OperationContext outer = OperationContext.start(OUTER_OPERATION)) {
                outer.withMessageId(OUTER_MESSAGE_ID).withAttempt(1);

                try (OperationContext inner = OperationContext.start(OPERATION)) {
                    inner.withMessageId(MESSAGE_ID).withAttempt(3);
                    assertThat(MDC.get(OperationContext.OPERATION_KEY)).isEqualTo(OPERATION);
                }

                // Without this, every line logged after a sub-operation would
                // lose the scan it belongs to.
                assertThat(MDC.get(OperationContext.OPERATION_KEY)).isEqualTo(OUTER_OPERATION);
                assertThat(MDC.get(OperationContext.MESSAGE_ID_KEY)).isEqualTo(OUTER_MESSAGE_ID);
                assertThat(MDC.get(OperationContext.ATTEMPT_KEY)).isEqualTo("1");
            }
        }

        @Test
        @DisplayName("does not inherit the enclosing operation's message id or attempt")
        void doesNotInheritEnclosingDetails() {
            try (OperationContext outer = OperationContext.start(OUTER_OPERATION)) {
                outer.withMessageId(OUTER_MESSAGE_ID).withAttempt(4);
                outer.succeeded();

                try (OperationContext inner = OperationContext.start(OPERATION)) {
                    // Inheriting would make the inner operation's lines claim a
                    // message and an attempt that belong to the outer one.
                    assertThat(MDC.get(OperationContext.MESSAGE_ID_KEY)).isNull();
                    assertThat(MDC.get(OperationContext.ATTEMPT_KEY)).isNull();
                    assertThat(MDC.get(OperationContext.OUTCOME_KEY)).isNull();
                }
            }
        }

        @Test
        @DisplayName("clears every key once the outermost context closes")
        void clearsEverythingAtTheOutermostClose() {
            try (OperationContext outer = OperationContext.start(OUTER_OPERATION)) {
                try (OperationContext inner = OperationContext.start(OPERATION)) {
                    inner.withMessageId(MESSAGE_ID);
                }
            }

            assertThat(MDC.get(OperationContext.OPERATION_KEY)).isNull();
            assertThat(MDC.get(OperationContext.MESSAGE_ID_KEY)).isNull();
            assertThat(MDC.get(OperationContext.ATTEMPT_KEY)).isNull();
            assertThat(MDC.get(OperationContext.ELAPSED_MILLIS_KEY)).isNull();
            assertThat(MDC.get(OperationContext.OUTCOME_KEY)).isNull();
            assertThat(MDC.get(OperationContext.FAILURE_KEY)).isNull();
        }
    }

    @Nested
    @DisplayName("rejects input that would produce a misleading log line")
    class RejectsBadInput {

        @Test
        @DisplayName("a null operation name")
        void nullOperation() {
            assertThatNullPointerException()
                    .isThrownBy(() -> OperationContext.start(null))
                    .withMessageContaining("operation");
        }

        @Test
        @DisplayName("a blank operation name")
        void blankOperation() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> OperationContext.start("   "))
                    .withMessageContaining("must not be blank");
        }

        @Test
        @DisplayName("an attempt below one, since the first try is attempt 1")
        void attemptBelowOne() {
            try (OperationContext operation = OperationContext.start(OPERATION)) {
                assertThatIllegalArgumentException()
                        .isThrownBy(() -> operation.withAttempt(0))
                        .withMessageContaining("at least 1");
            }
        }
    }

    @Test
    @DisplayName("closing twice does not overwrite the enclosing context a second time")
    void closingTwiceIsIdempotent() {
        try (OperationContext outer = OperationContext.start(OUTER_OPERATION)) {
            OperationContext inner = OperationContext.start(OPERATION);
            inner.close();

            MDC.put(OperationContext.MESSAGE_ID_KEY, OUTER_MESSAGE_ID);
            inner.close();

            // A second restore would clobber what the outer context set after
            // the first close.
            assertThat(MDC.get(OperationContext.MESSAGE_ID_KEY)).isEqualTo(OUTER_MESSAGE_ID);
        }
    }

    @Test
    @DisplayName("confines context to the thread that opened it")
    void confinesContextToItsOwnThread() throws InterruptedException {
        AtomicReference<String> seenOnOtherThread = new AtomicReference<>();
        CountDownLatch finished = new CountDownLatch(1);

        try (OperationContext operation = OperationContext.start(OPERATION)) {
            Thread other = new Thread(() -> {
                seenOnOtherThread.set(MDC.get(OperationContext.OPERATION_KEY));
                finished.countDown();
            });
            other.start();
            assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(seenOnOtherThread.get()).isNull();
    }
}
