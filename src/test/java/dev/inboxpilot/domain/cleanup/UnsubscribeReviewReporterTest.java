package dev.inboxpilot.domain.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.analysis.UnsubscribeCapabilities;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MessageId;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UnsubscribeReviewReporter")
class UnsubscribeReviewReporterTest {

    @Test
    @DisplayName("groups and deduplicates manual capabilities by sender")
    void groupsCapabilities() {
        EmailAddress sender = new EmailAddress("news@example.com");
        URI mailto = URI.create("mailto:leave@example.com");
        URI https = URI.create("https://example.com/leave");
        UnsubscribeCapabilities capabilities = new UnsubscribeCapabilities(
                List.of(mailto), List.of(https));

        UnsubscribeReviewReport report = new UnsubscribeReviewReporter().create(List.of(
                new UnsubscribeMessage(new MessageId("m2"), sender, capabilities),
                new UnsubscribeMessage(new MessageId("m1"), sender, capabilities)));

        assertThat(report.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.messageIds()).containsExactly(new MessageId("m1"), new MessageId("m2"));
            assertThat(entry.mailto()).containsExactly(mailto);
            assertThat(entry.https()).containsExactly(https);
        });
    }
}
