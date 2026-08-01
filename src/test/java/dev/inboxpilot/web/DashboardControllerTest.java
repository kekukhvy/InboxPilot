package dev.inboxpilot.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.inboxpilot.application.dashboard.DashboardQueryService;
import dev.inboxpilot.application.model.DashboardDocument;
import dev.inboxpilot.application.model.DashboardDocumentSummary;
import dev.inboxpilot.application.port.DashboardDocumentStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DashboardControllerTest {

    private MockMvc dashboard;

    @BeforeEach
    void setUp() {
        DashboardQueryService service = new DashboardQueryService(new StubStore());
        dashboard = MockMvcBuilders.standaloneSetup(new DashboardController(service)).build();
    }

    @Test
    void servesTheReviewPageWithRestrictiveBrowserPolicy() throws Exception {
        dashboard.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("object-src 'none'")));
    }

    @Test
    void exposesReportMetadataAndContentThroughGetOnlyRoutes() throws Exception {
        dashboard.perform(get("/dashboard/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("inventory.json"));
        dashboard.perform(get("/dashboard/api/documents/report-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("{}"));
        dashboard.perform(post("/dashboard/api/documents/report-id"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void returnsNotFoundForAnUnknownOpaqueId() throws Exception {
        dashboard.perform(get("/dashboard/api/documents/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(
                        "Dashboard document not found: missing"));
    }

    private static final class StubStore implements DashboardDocumentStore {
        private static final DashboardDocumentSummary SUMMARY =
                new DashboardDocumentSummary("report-id", "REPORT", "inventory.json", 2, Instant.EPOCH);
        private static final DashboardDocument DOCUMENT =
                new DashboardDocument("inventory.json", "application/json", "{}");

        @Override
        public List<DashboardDocumentSummary> list() {
            return List.of(SUMMARY);
        }

        @Override
        public Optional<DashboardDocument> findById(String id) {
            return SUMMARY.id().equals(id) ? Optional.of(DOCUMENT) : Optional.empty();
        }
    }
}
