package dev.inboxpilot.application.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ScanFingerprint")
class ScanFingerprintTest {

    private static final String CONFIGURATION = "query=after:1;spam=false;trash=false";

    @Test
    @DisplayName("is deterministic for the canonical scan configuration")
    void isDeterministic() {
        assertThat(ScanFingerprint.fromCanonical(CONFIGURATION))
                .isEqualTo(ScanFingerprint.fromCanonical(CONFIGURATION));
    }
}
