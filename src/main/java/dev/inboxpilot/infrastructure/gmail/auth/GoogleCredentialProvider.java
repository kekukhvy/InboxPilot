package dev.inboxpilot.infrastructure.gmail.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import dev.inboxpilot.application.model.AccessToken;
import dev.inboxpilot.application.port.CredentialProvider;
import dev.inboxpilot.application.port.CredentialProviderException;
import dev.inboxpilot.domain.error.Failure;
import dev.inboxpilot.infrastructure.config.InboxPilotProperties;
import dev.inboxpilot.infrastructure.config.OAuthProperties;
import dev.inboxpilot.infrastructure.observability.OperationContext;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Desktop OAuth authorization for Gmail (issue #9).
 *
 * <p>The first call opens the user's browser at Google's consent screen via a
 * local loopback server ({@link LocalServerReceiver}); every later call reuses
 * the refresh token cached in {@link OAuthProperties#tokenStore()} and only
 * talks to the network when the access token has expired. Only this class and
 * its {@code auth} package siblings know about Google client types — the
 * {@link AccessToken} handed back is a plain domain value (ADR 0001, enforced
 * by {@code ArchitectureTest}).
 *
 * <p>The scope requested is exactly {@link OAuthProperties#scopes()} — nothing
 * is added implicitly, so the least-privilege default in {@code
 * application.yml} (AC1 of issue #9) is what actually gets requested.
 *
 * <p>Credential resolution, the token store directory, and error translation
 * are unit-tested in isolation ({@link OAuthCredentialGuard},
 * {@link TokenStoreDirectory}, {@link OAuthFailureTranslator}); the browser
 * round-trip itself cannot run in a unit test and is kept to the two calls
 * below.
 */
@Component
@SuppressWarnings("deprecation") // GoogleNetHttpTransport is the documented way to build a
// trusted transport for AuthorizationCodeInstalledApp in this client library version; its
// replacement is not exposed by a stable, equivalent API here.
public class GoogleCredentialProvider implements CredentialProvider {

    private static final String OPERATION = "gmail.oauth.authorize";
    private static final String LOCAL_USER_ID = "inboxpilot";
    private static final String OFFLINE_ACCESS_TYPE = "offline";

    /** Gmail access tokens are typically valid for one hour; used only if Google omits the field. */
    private static final long DEFAULT_TOKEN_LIFETIME_SECONDS = 3_600L;

    private static final Logger logger = LoggerFactory.getLogger(GoogleCredentialProvider.class);

    private final InboxPilotProperties properties;

    public GoogleCredentialProvider(InboxPilotProperties properties) {
        this.properties = properties;
    }

    @Override
    public AccessToken obtainAccessToken() {
        try (OperationContext operation = OperationContext.start(OPERATION)) {
            try {
                AccessToken token = authorize(OAuthCredentialGuard.requireConfigured(properties::oauth));
                operation.succeeded();
                logger.info("Gmail authorization succeeded; access token expires at {}",
                        token.expiresAt());
                return token;
            } catch (CredentialProviderException e) {
                // The only place an authorization failure is logged: it is recorded here,
                // with the operation context still attached, and rethrown untouched so
                // callers see it exactly once.
                operation.failed(Failure.PERMANENT);
                logger.error("Gmail authorization failed: {}", e.getMessage(), e);
                throw e;
            }
        }
    }

    private AccessToken authorize(OAuthProperties configured) {
        try {
            Credential credential = runAuthorizationFlow(configured);
            return toAccessToken(credential);
        } catch (IOException | GeneralSecurityException e) {
            throw OAuthFailureTranslator.translateIfPossible(e);
        }
    }

    private Credential runAuthorizationFlow(OAuthProperties configured)
            throws IOException, GeneralSecurityException {
        HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        GoogleAuthorizationCodeFlow flow = buildFlow(configured, transport, jsonFactory);
        logger.info("Opening browser for Gmail authorization consent; awaiting user action");
        LocalServerReceiver receiver = new LocalServerReceiver();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize(LOCAL_USER_ID);
    }

    private GoogleAuthorizationCodeFlow buildFlow(
            OAuthProperties configured, HttpTransport transport, GsonFactory jsonFactory)
            throws IOException {
        GoogleClientSecrets clientSecrets = clientSecrets(configured);
        File tokenDirectory = TokenStoreDirectory.prepare(configured.tokenStore()).toFile();
        logger.debug("Authorizing client {} for scopes {}; caching the refresh token in {}",
                configured.clientId(), configured.scopes(), tokenDirectory);
        return new GoogleAuthorizationCodeFlow.Builder(
                transport, jsonFactory, clientSecrets, configured.scopes())
                .setDataStoreFactory(new FileDataStoreFactory(tokenDirectory))
                .setAccessType(OFFLINE_ACCESS_TYPE)
                .build();
    }

    private static GoogleClientSecrets clientSecrets(OAuthProperties configured) {
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details()
                .setClientId(configured.clientId())
                .setClientSecret(configured.clientSecret());
        return new GoogleClientSecrets().setInstalled(details);
    }

    private static AccessToken toAccessToken(Credential credential) {
        Long expiresInSeconds = credential.getExpiresInSeconds();
        long secondsUntilExpiry = expiresInSeconds != null ? expiresInSeconds : DEFAULT_TOKEN_LIFETIME_SECONDS;
        Instant expiresAt = Instant.now().plusSeconds(secondsUntilExpiry);
        return new AccessToken(credential.getAccessToken(), expiresAt);
    }
}
