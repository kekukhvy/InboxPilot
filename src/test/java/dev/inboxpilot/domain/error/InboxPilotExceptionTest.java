package dev.inboxpilot.domain.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Covers AC1 of issue #5 — transient and permanent failures are distinguishable.
 *
 * <p>Plain JUnit with no Spring context and no mailbox, which is the property
 * ADR 0001 exists to protect for {@code domain} code.
 */
@DisplayName("Error model")
class InboxPilotExceptionTest {

    private static final String MESSAGE = "Gmail returned 429";
    private static final String PERMANENT_MESSAGE = "OAuth token was revoked";

    @Nested
    @DisplayName("a transient failure")
    class TransientFailure {

        @Test
        @DisplayName("is classified transient and is retryable")
        void isRetryable() {
            TransientFailureException failure = new TransientFailureException(MESSAGE);

            assertThat(failure.failure()).isEqualTo(Failure.TRANSIENT);
            assertThat(failure.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("keeps the message and the underlying cause for diagnosis")
        void keepsMessageAndCause() {
            Throwable cause = new IllegalStateException("connection reset");

            TransientFailureException failure = new TransientFailureException(MESSAGE, cause);

            assertThat(failure.getMessage()).isEqualTo(MESSAGE);
            assertThat(failure.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("a permanent failure")
    class PermanentFailure {

        @Test
        @DisplayName("is classified permanent and is not retryable")
        void isNotRetryable() {
            PermanentFailureException failure = new PermanentFailureException(PERMANENT_MESSAGE);

            assertThat(failure.failure()).isEqualTo(Failure.PERMANENT);
            assertThat(failure.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("keeps the message and the underlying cause for diagnosis")
        void keepsMessageAndCause() {
            Throwable cause = new IllegalArgumentException("invalid_grant");

            PermanentFailureException failure =
                    new PermanentFailureException(PERMANENT_MESSAGE, cause);

            assertThat(failure.getMessage()).isEqualTo(PERMANENT_MESSAGE);
            assertThat(failure.getCause()).isSameAs(cause);
        }
    }

    @Test
    @DisplayName("lets a caller branch on retryability without knowing the concrete type")
    void branchesOnTheBaseType() {
        InboxPilotException transientFailure = new TransientFailureException(MESSAGE);
        InboxPilotException permanentFailure = new PermanentFailureException(PERMANENT_MESSAGE);

        // The point of the hierarchy: retry sites depend on this question only,
        // so a new failure type never forces them to change.
        assertThat(transientFailure.isRetryable()).isTrue();
        assertThat(permanentFailure.isRetryable()).isFalse();
    }

    @Test
    @DisplayName("is unchecked, so it can cross call sites that cannot recover")
    void isUnchecked() {
        assertThat(RuntimeException.class).isAssignableFrom(InboxPilotException.class);
    }

    @Test
    @DisplayName("rejects a missing classification rather than defaulting to one")
    void rejectsNullClassification() {
        // Guessing here would silently make an unclassified failure retryable
        // (or not) and burn quota either way.
        assertThatNullPointerException()
                .isThrownBy(() -> new InboxPilotException(null, MESSAGE) { })
                .withMessageContaining("failure");
    }

    @Nested
    @DisplayName("Failure")
    class FailureClassification {

        @Test
        @DisplayName("marks only TRANSIENT as worth retrying")
        void onlyTransientIsRetryable() {
            assertThat(Failure.TRANSIENT.isRetryable()).isTrue();
            assertThat(Failure.PERMANENT.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("covers every failure with exactly one of the two answers")
        void hasNoThirdState() {
            // A third constant would leave every retry site with an unhandled
            // case, so the exhaustiveness is worth pinning down.
            assertThat(Failure.values())
                    .containsExactly(Failure.TRANSIENT, Failure.PERMANENT);
        }
    }
}
