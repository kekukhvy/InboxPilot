package dev.inboxpilot.application.port;

import dev.inboxpilot.domain.message.MailMessage;
import java.util.List;

/**
 * Persists and restores the message-level snapshot inventory produces and
 * classification consumes.
 *
 * <p>The snapshot exists so that a classification dry run never has to contact
 * the mailbox: it retains the per-message data reports need — id, sender,
 * subject, received timestamp, and label ids — which the sender/domain
 * aggregates in {@code inventory.json} deliberately do not keep (issue #151).
 *
 * <p>Writes happen at completed batch boundaries, so an interrupted scan keeps
 * whatever batches already finished.
 */
public interface MessageSnapshotStore {

    /**
     * Replaces the persisted snapshot with the given messages.
     *
     * @param messages every message captured so far, oldest batch first
     * @throws MessageSnapshotStoreException if the snapshot cannot be written
     */
    void write(List<MailMessage> messages);

    /**
     * Loads the persisted message snapshot.
     *
     * @return the snapshotted messages
     * @throws MessageSnapshotStoreException if no compatible snapshot exists,
     *         carrying an actionable instruction to rerun inventory
     */
    List<MailMessage> load();
}
