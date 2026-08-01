package dev.inboxpilot.application.port;

import dev.inboxpilot.application.model.AccessToken;

/**
 * Port for obtaining an authorized Gmail {@link AccessToken}.
 *
 * <p>Declared by the application layer and expressed purely in domain/JDK
 * terms, so use cases never see a Google {@code Credential} or learn how
 * authorization happened — desktop browser flow, service account, or anything
 * else. Implemented by an adapter in {@code dev.inboxpilot.infrastructure} —
 * see ADR 0001.
 */
public interface CredentialProvider {

    /**
     * Returns a valid access token, authorizing interactively (via a local
     * browser flow) the first time and reusing a persisted refresh token on
     * later calls.
     *
     * @return a non-expired access token
     * @throws CredentialProviderException if authorization is not configured,
     *                                      the token store cannot be used, or
     *                                      the credentials are invalid
     */
    AccessToken obtainAccessToken();
}
