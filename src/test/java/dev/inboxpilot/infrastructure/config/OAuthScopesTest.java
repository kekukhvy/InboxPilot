package dev.inboxpilot.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Covers AC1 of issue #9 — the shipped default requests only the minimum scope
 * an inventory scan needs.
 *
 * <p>{@code gmail.modify} is required only once cleanup (label/archive) ships;
 * shipping it by default would grant a v0.2 inventory run more access than it
 * uses. Enabling it is left as a deliberate {@code application.yml} edit, not
 * something this build turns on for you.
 */
@SpringBootTest
@DisplayName("OAuth scopes")
class OAuthScopesTest {

    private static final String READONLY_SCOPE =
            "https://www.googleapis.com/auth/gmail.readonly";
    private static final String MODIFY_SCOPE =
            "https://www.googleapis.com/auth/gmail.modify";

    @Autowired
    private InboxPilotProperties properties;

    @Test
    @DisplayName("ships gmail.readonly as the only default scope")
    void shipsReadOnlyAsTheOnlyDefaultScope() {
        List<String> scopes = properties.oauth().scopes();

        assertThat(scopes)
                .as("least privilege: the inventory slice never needs write access")
                .containsExactly(READONLY_SCOPE)
                .doesNotContain(MODIFY_SCOPE);
    }
}
