package dev.inboxpilot.domain.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.inboxpilot.domain.message.EmailAddress;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiInputBuilder")
class AiInputBuilderTest {

    private static final AiMessageMetadata METADATA = new AiMessageMetadata(
            new EmailAddress("private.person@example.com"),
            "Contact private@example.net at https://secret.example/path with code 123456",
            List.of("Label_Private"));

    @Test
    @DisplayName("requires opt-in before creating any outbound payload")
    void requiresOptIn() {
        assertThatThrownBy(() -> new AiInputBuilder().build(METADATA, AiPrivacyPolicy.disabled()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("opt-in");
    }

    @Test
    @DisplayName("shares only sender domain and explicitly enabled locally redacted fields")
    void buildsRedactedInput() {
        AiPrivacyPolicy policy = new AiPrivacyPolicy(true, true, false);

        AiClassificationInput input = new AiInputBuilder().build(METADATA, policy);

        assertThat(input.senderDomain()).isEqualTo("example.com");
        assertThat(input.redactedSubject()).contains(
                "Contact [email] at [url] with code [number]");
        assertThat(input.labelIds()).isEmpty();
        assertThat(input.toString()).doesNotContain(
                "private.person", "private@example.net", "secret.example", "123456", "Label_Private");
    }
}
