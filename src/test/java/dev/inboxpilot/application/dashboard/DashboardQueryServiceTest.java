package dev.inboxpilot.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.inboxpilot.application.model.DashboardDocument;
import dev.inboxpilot.application.model.DashboardDocumentSummary;
import dev.inboxpilot.application.port.DashboardDocumentStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DashboardQueryServiceTest {

    private static final DashboardDocumentSummary SUMMARY =
            new DashboardDocumentSummary("report-id", "REPORT", "inventory.json", 2, Instant.EPOCH);
    private static final DashboardDocument DOCUMENT =
            new DashboardDocument("inventory.json", "application/json", "{}");

    @Test
    void exposesReadOnlyDocumentQueries() {
        DashboardQueryService service = new DashboardQueryService(new StubStore());

        assertThat(service.list()).containsExactly(SUMMARY);
        assertThat(service.get("report-id")).isEqualTo(DOCUMENT);
    }

    @Test
    void reportsAnUnknownDocumentWithoutInventingContent() {
        DashboardQueryService service = new DashboardQueryService(new StubStore());

        assertThatThrownBy(() -> service.get("missing"))
                .isInstanceOf(DashboardDocumentNotFoundException.class)
                .hasMessageContaining("missing");
    }

    private static final class StubStore implements DashboardDocumentStore {
        @Override
        public List<DashboardDocumentSummary> list() {
            return List.of(SUMMARY);
        }

        @Override
        public Optional<DashboardDocument> findById(String id) {
            return "report-id".equals(id) ? Optional.of(DOCUMENT) : Optional.empty();
        }
    }
}
