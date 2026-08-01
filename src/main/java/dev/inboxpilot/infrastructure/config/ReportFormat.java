package dev.inboxpilot.infrastructure.config;

/**
 * Output formats a report can be written in.
 *
 * <p>A closed set, so an unknown value in {@code application.yml} fails at
 * startup with the accepted values listed — rather than producing a report
 * nobody can open.
 */
public enum ReportFormat {

    /** Machine-readable, for piping into other tools. */
    JSON,

    /** Spreadsheet-friendly, one row per record. */
    CSV,

    /** Human-readable summary for reading in a browser. */
    HTML
}
