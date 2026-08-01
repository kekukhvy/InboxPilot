package dev.inboxpilot.infrastructure.gmail.auth;

import dev.inboxpilot.application.port.CredentialProviderException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prepares the directory that holds the persisted Gmail refresh token
 * ({@code inboxpilot.oauth.token-store}), keeping it outside the source tree
 * and readable by its owner only.
 *
 * <p>The refresh token is a long-lived secret: anyone who can read the file
 * can act as the user's mailbox. Owner-only permissions are set where the
 * filesystem supports POSIX permissions and skipped elsewhere — a Windows or
 * FAT filesystem simply has no equivalent, so this degrades to "as private as
 * the platform allows" instead of failing (project invariant: degrade, don't
 * fail).
 */
final class TokenStoreDirectory {

    private static final String TOKEN_STORE_KEY = "inboxpilot.oauth.token-store";
    private static final String TOKEN_STORE_ENV_VAR = "INBOXPILOT_TOKEN_STORE";
    private static final String POSIX_VIEW = "posix";

    private static final Set<PosixFilePermission> OWNER_ONLY_PERMISSIONS =
            PosixFilePermissions.fromString("rwx------");

    private static final Logger logger = LoggerFactory.getLogger(TokenStoreDirectory.class);

    private TokenStoreDirectory() {
    }

    /**
     * Creates the token store directory if it does not exist yet, and ensures
     * it is owner-only where the filesystem supports it.
     *
     * @param configured the directory from {@code inboxpilot.oauth.token-store}
     * @return the prepared, absolute directory path
     * @throws CredentialProviderException if the directory cannot be created,
     *                                      naming the configuration key and the
     *                                      environment variable that supplies it
     */
    static Path prepare(Path configured) {
        Path resolved = configured.toAbsolutePath();
        logger.debug("Token store directory configured as {}", resolved);
        createDirectory(resolved);
        restrictToOwnerWherePosix(resolved);
        return resolved;
    }

    private static void createDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
            logger.debug("Token store directory created at {}", directory);
        } catch (IOException e) {
            logger.error("Failed to create token store directory at {}", directory, e);
            throw new CredentialProviderException(
                    TOKEN_STORE_KEY + " could not be created at " + directory
                            + ". Point " + TOKEN_STORE_KEY + " (environment variable "
                            + TOKEN_STORE_ENV_VAR + ") at a writable directory.", e);
        }
    }

    private static void restrictToOwnerWherePosix(Path directory) {
        if (!directory.getFileSystem().supportedFileAttributeViews().contains(POSIX_VIEW)) {
            logger.warn("Filesystem at {} has no POSIX permissions; token store directory "
                    + "permissions are left at the platform default (less secure)", directory);
            return;
        }
        try {
            Files.setPosixFilePermissions(directory, OWNER_ONLY_PERMISSIONS);
            logger.debug("Token store directory restricted to owner-only access");
        } catch (IOException e) {
            logger.error("Failed to restrict token store directory to owner-only access at {}", directory, e);
            throw new CredentialProviderException(
                    TOKEN_STORE_KEY + " at " + directory
                            + " could not be restricted to owner-only access.", e);
        }
    }
}
