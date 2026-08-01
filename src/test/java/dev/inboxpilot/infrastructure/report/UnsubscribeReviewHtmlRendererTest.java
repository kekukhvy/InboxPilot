package dev.inboxpilot.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.cleanup.UnsubscribeReviewEntry;
import dev.inboxpilot.domain.cleanup.UnsubscribeReviewReport;
import dev.inboxpilot.domain.message.EmailAddress;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UnsubscribeReviewHtmlRenderer")
class UnsubscribeReviewHtmlRendererTest {

    @Test
    @DisplayName("presents mailto and HTTPS links with a manual-review warning")
    void rendersManualActions() {
        UnsubscribeReviewEntry entry = new UnsubscribeReviewEntry(
                new EmailAddress("news@example.com"), List.of(),
                List.of(URI.create("mailto:leave@example.com")),
                List.of(URI.create("https://example.com/leave?a=1&b=2")));

        String html = new UnsubscribeReviewHtmlRenderer()
                .render(new UnsubscribeReviewReport(List.of(entry)));

        assertThat(html)
                .contains("href=\"mailto:leave@example.com\"")
                .contains("href=\"https://example.com/leave?a=1&amp;b=2\"")
                .contains("has not invoked any link");
    }
}
