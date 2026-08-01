package dev.inboxpilot.domain.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("MessageId")
class MessageIdTest {

    private static final String RAW_ID = "18f2a1c9d0";
    private static final String OTHER_RAW_ID = "18f2a1c9d1";

    @Test
    @DisplayName("keeps the identifier the provider issued, unchanged")
    void keepsProviderValue() {
        // Unlike EmailAddress, an id is opaque: normalising it would risk
        // turning a valid provider id into one that matches nothing.
        assertThat(new MessageId(RAW_ID).value()).isEqualTo(RAW_ID);
    }

    @Test
    @DisplayName("compares by value, so the same message is the same id")
    void comparesByValue() {
        assertThat(new MessageId(RAW_ID))
                .isEqualTo(new MessageId(RAW_ID))
                .hasSameHashCodeAs(new MessageId(RAW_ID))
                .isNotEqualTo(new MessageId(OTHER_RAW_ID));
    }

    @Test
    @DisplayName("prints as the bare identifier, so log lines stay readable")
    void printsAsBareValue() {
        // Without this, every log line and MDC entry would read
        // "MessageId[value=...]" instead of the id itself.
        assertThat(new MessageId(RAW_ID)).hasToString(RAW_ID);
    }

    @Test
    @DisplayName("rejects a missing identifier")
    void rejectsNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new MessageId(null))
                .withMessageContaining("value");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("rejects a blank identifier, which would silently match nothing")
    void rejectsBlank(String blank) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MessageId(blank))
                .withMessageContaining("must not be blank");
    }
}
