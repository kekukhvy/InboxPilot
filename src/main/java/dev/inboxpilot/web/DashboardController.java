package dev.inboxpilot.web;

import dev.inboxpilot.application.dashboard.DashboardDocumentNotFoundException;
import dev.inboxpilot.application.dashboard.DashboardQueryService;
import dev.inboxpilot.application.model.DashboardDocument;
import dev.inboxpilot.application.model.DashboardDocumentSummary;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Read-only HTTP presentation for local reports and deterministic rules. */
@RestController
@ConditionalOnProperty(prefix = "inboxpilot.dashboard", name = "enabled", havingValue = "true")
public class DashboardController {

    private static final String CONTENT_SECURITY_POLICY =
            "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
                    + "img-src 'none'; connect-src 'self'; frame-src 'none'; object-src 'none'";
    private static final String CONTENT_SECURITY_POLICY_HEADER = "Content-Security-Policy";
    private final DashboardQueryService queryService;

    public DashboardController(DashboardQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> index() {
        Resource page = new ClassPathResource("dashboard/index.html");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY)
                .body(page);
    }

    @GetMapping("/dashboard/api/documents")
    public List<DashboardDocumentSummary> documents() {
        return queryService.list();
    }

    @GetMapping("/dashboard/api/documents/{id}")
    public DashboardDocument document(@PathVariable String id) {
        return queryService.get(id);
    }

    @ExceptionHandler(DashboardDocumentNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(
            DashboardDocumentNotFoundException exception) {
        return ResponseEntity.status(404)
                .body(Map.of("error", exception.getMessage()));
    }
}
