package dev.inboxpilot.domain.analysis;

/** Non-mutating kinds of proposed label taxonomy changes. */
public enum LabelTaxonomyChangeType {
    CREATE,
    MERGE,
    RENAME
}
