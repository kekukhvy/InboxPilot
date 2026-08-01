package dev.inboxpilot.domain.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ListUnsubscribeParser")
class ListUnsubscribeParserTest {

    @Test
    @DisplayName("extracts mailto and HTTPS capabilities without invoking them")
    void extractsSupportedCapabilities() {
        String header = "<mailto:leave@example.com?subject=unsubscribe>, <https://example.com/leave>";

        UnsubscribeCapabilities capabilities = new ListUnsubscribeParser().parse(header);

        assertThat(capabilities.mailto()).containsExactly(URI.create("mailto:leave@example.com?subject=unsubscribe"));
        assertThat(capabilities.https()).containsExactly(URI.create("https://example.com/leave"));
    }

    @Test
    @DisplayName("ignores malformed and unsafe mechanisms")
    void ignoresUnsupportedMechanisms() {
        UnsubscribeCapabilities capabilities =
                new ListUnsubscribeParser().parse("<http://example.com>, <javascript:alert(1)>, broken");

        assertThat(capabilities.mailto()).isEmpty();
        assertThat(capabilities.https()).isEmpty();
    }
}
