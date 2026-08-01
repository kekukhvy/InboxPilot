package dev.inboxpilot.domain.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MessageId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StaleNewsletterDetector")
class StaleNewsletterDetectorTest {

    private static final Instant STALE_BEFORE = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("groups only newsletter messages older than the configured threshold")
    void groupsStaleNewslettersBySender() {
        List<NewsletterMessage> messages = List.of(
                message("old-2", "digest@example.com", STALE_BEFORE.minusSeconds(2), true),
                message("old-1", "digest@example.com", STALE_BEFORE.minusSeconds(1), true),
                message("recent", "digest@example.com", STALE_BEFORE, true),
                message("personal", "friend@example.com", STALE_BEFORE.minusSeconds(1), false));

        List<StaleNewsletterGroup> groups = new StaleNewsletterDetector()
                .detect(messages, STALE_BEFORE);

        assertThat(groups).singleElement().satisfies(group -> {
            assertThat(group.sender()).isEqualTo(new EmailAddress("digest@example.com"));
            assertThat(group.messageIds()).containsExactly(new MessageId("old-2"), new MessageId("old-1"));
            assertThat(group.oldestReceivedAt()).isEqualTo(STALE_BEFORE.minusSeconds(2));
            assertThat(group.newestReceivedAt()).isEqualTo(STALE_BEFORE.minusSeconds(1));
        });
    }

    private static NewsletterMessage message(
            String id, String sender, Instant receivedAt, boolean newsletter) {
        return new NewsletterMessage(
                new MessageId(id), new EmailAddress(sender), receivedAt, newsletter);
    }
}
