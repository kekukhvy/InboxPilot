package dev.inboxpilot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Verifies that the application's composition root is wired correctly and the
 * Spring context starts — the "application starts locally" acceptance criterion
 * of issue #2, asserted rather than assumed.
 */
@SpringBootTest
@DisplayName("InboxPilot application")
class InboxPilotApplicationTest {

    private static final String APPLICATION_BEAN_NAME = "inboxPilotApplication";

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("starts and exposes a running application context")
    void startsApplicationContext() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBeanDefinitionCount()).isPositive();
    }

    @Test
    @DisplayName("registers the composition root as a bean")
    void registersCompositionRoot() {
        assertThat(applicationContext.containsBean(APPLICATION_BEAN_NAME)).isTrue();
        assertThat(applicationContext.getBean(InboxPilotApplication.class)).isNotNull();
    }
}
