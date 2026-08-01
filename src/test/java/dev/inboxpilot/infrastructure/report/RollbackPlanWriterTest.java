package dev.inboxpilot.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.classification.RollbackLabelChange;
import dev.inboxpilot.domain.classification.RollbackPlan;
import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.infrastructure.config.ReportFormat;
import dev.inboxpilot.infrastructure.config.ReportProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("RollbackPlanWriter")
class RollbackPlanWriterTest {

    @TempDir
    Path directory;

    @Test
    @DisplayName("writes a versioned deterministic JSON rollback artifact")
    void writesRollbackArtifact() throws Exception {
        RollbackPlan plan = new RollbackPlan(1, List.of(new RollbackLabelChange(
                new MessageId("m1"), List.of("Label_Old"), List.of("Label_New"))));
        ReportProperties properties = new ReportProperties(
                directory, List.of(ReportFormat.JSON), false);

        Path path = new RollbackPlanWriter(properties).write(plan);

        assertThat(path.getFileName().toString()).isEqualTo("classification-rollback.json");
        assertThat(Files.readString(path)).isEqualTo(
                "{\"version\":1,\"changes\":[{\"messageId\":\"m1\","
                        + "\"addLabelIds\":[\"Label_Old\"],\"removeLabelIds\":[\"Label_New\"]}]}\n");
    }
}
