package dev.inboxpilot.application.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Builds a stable fingerprint from a canonical scan configuration and query. */
public final class ScanFingerprint {

    private static final String ALGORITHM = "SHA-256";
    private static final String ALGORITHM_FAILURE = "Required digest is unavailable";

    private ScanFingerprint() {
    }

    public static String fromCanonical(String canonicalConfiguration) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] value = digest.digest(canonicalConfiguration.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(ALGORITHM_FAILURE, exception);
        }
    }
}
