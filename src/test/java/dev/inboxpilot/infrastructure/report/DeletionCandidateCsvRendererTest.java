package dev.inboxpilot.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.cleanup.DeletionCandidate;
import dev.inboxpilot.domain.cleanup.DeletionCandidateCategory;
import dev.inboxpilot.domain.cleanup.DeletionCandidateReport;
import dev.inboxpilot.domain.message.MessageId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DeletionCandidateCsvRenderer")
class DeletionCandidateCsvRendererTest {

    @Test
    @DisplayName("exports candidate evidence without an execution field")
    void exportsCandidates() {
        DeletionCandidate candidate = new DeletionCandidate(
                new MessageId("m1"), DeletionCandidateCategory.OTP, "one-time code");

        String csv = new DeletionCandidateCsvRenderer()
                .render(new DeletionCandidateReport(List.of(candidate)));

        assertThat(csv).contains("messageId,category,reason", "\"m1\",\"OTP\",\"one-time code\"")
                .doesNotContain("execute", "deleteUrl");
    }
}
