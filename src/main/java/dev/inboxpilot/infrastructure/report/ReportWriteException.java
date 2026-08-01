package dev.inboxpilot.infrastructure.report;

/** Reports a failure to create a configured inventory report. */
public class ReportWriteException extends RuntimeException {

    public ReportWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
