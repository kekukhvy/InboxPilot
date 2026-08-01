package dev.inboxpilot.application.port;

import dev.inboxpilot.application.model.LabelBatchChange;

/** Applies an explicit batch containing only user-label changes. */
public interface LabelChangeGateway {

    void apply(LabelBatchChange change);
}
