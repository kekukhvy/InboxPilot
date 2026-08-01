package dev.inboxpilot.infrastructure.gmail.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import dev.inboxpilot.application.port.CredentialProviderException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers AC2 of issue #9 — the refresh token is persisted outside the source
 * tree, in a directory only the owner can read.
 *
 * <p>Filesystem permissions are checked when the test filesystem supports
 * POSIX permissions and skipped, with a warning rather than a failure,
 * otherwise — the "degrade, don't fail" invariant applied to a platform detail
 * that varies between CI runners.
 */
@DisplayName("TokenStoreDirectory")
class TokenStoreDirectoryTest {

    private static final String PROJECT_MARKER = "build.gradle.kts";

    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("resolves to a directory outside the project source tree")
    void resolvesOutsideProjectDirectory() {
        Path projectRoot = Path.of("").toAbsolutePath();
        Path store = tempDir.resolve("tokens");

        Path resolved = TokenStoreDirectory.prepare(store);

        assertThat(resolved).isAbsolute();
        assertThat(resolved.startsWith(projectRoot))
                .as("token store must never live inside the repository: %s", projectRoot)
                .isFalse();
        assertThat(Files.exists(projectRoot.resolve(PROJECT_MARKER)))
                .as("sanity check: the project marker must exist where expected")
                .isTrue();
    }

    @Test
    @DisplayName("creates the directory when it does not exist yet")
    void createsMissingDirectory() {
        Path store = tempDir.resolve("nested/tokens");

        Path resolved = TokenStoreDirectory.prepare(store);

        assertThat(Files.isDirectory(resolved)).isTrue();
    }

    @Test
    @DisplayName("restricts the directory to owner-only access where POSIX permissions apply")
    void restrictsToOwnerOnlyOnPosix() throws IOException {
        FileSystem fileSystem = tempDir.getFileSystem();
        if (!fileSystem.supportedFileAttributeViews().contains("posix")) {
            return;
        }
        Path store = tempDir.resolve("tokens");

        Path resolved = TokenStoreDirectory.prepare(store);

        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(resolved);
        assertThat(PosixFilePermissions.toString(permissions)).isEqualTo("rwx------");
    }

    @Test
    @DisplayName("reports a permanent, actionable failure when the directory cannot be created")
    void reportsFailureWhenDirectoryCannotBeCreated() throws IOException {
        Path blockingFile = tempDir.resolve("blocked");
        Files.createFile(blockingFile);
        Path store = blockingFile.resolve("tokens");

        assertThatExceptionOfType(CredentialProviderException.class)
                .isThrownBy(() -> TokenStoreDirectory.prepare(store))
                .withMessageContaining("inboxpilot.oauth.token-store")
                .withMessageContaining("INBOXPILOT_TOKEN_STORE");
    }
}
