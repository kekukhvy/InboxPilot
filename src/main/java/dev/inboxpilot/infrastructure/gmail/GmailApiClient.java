package dev.inboxpilot.infrastructure.gmail;

import java.io.IOException;
import java.util.List;

interface GmailApiClient {

    List<GmailLabel> listLabels() throws IOException;

    GmailMessagePage listMessageIds(String query, String pageToken) throws IOException;

    GmailMetadataBatch getMessageMetadata(List<String> messageIds) throws IOException;

    void batchModifyLabels(
            List<String> messageIds, List<String> addLabelIds, List<String> removeLabelIds)
            throws IOException;
}
