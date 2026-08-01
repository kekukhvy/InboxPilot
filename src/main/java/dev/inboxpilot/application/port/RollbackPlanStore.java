package dev.inboxpilot.application.port;

import dev.inboxpilot.domain.classification.RollbackPlan;

/** Persists inverse operations before any classification write is allowed. */
public interface RollbackPlanStore {

    void save(RollbackPlan plan);
}
