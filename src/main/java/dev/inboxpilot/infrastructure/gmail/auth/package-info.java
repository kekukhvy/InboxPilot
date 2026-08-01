/**
 * Desktop OAuth authorization for Gmail (issue #9).
 *
 * <p>Implements {@link dev.inboxpilot.application.port.CredentialProvider}
 * using Google's OAuth client libraries — the only package outside
 * {@code infrastructure.gmail}'s parent allowed to reference
 * {@code com.google.*} types (ADR 0001, enforced by {@code ArchitectureTest}).
 * The interactive browser round-trip is kept thin; scope resolution, the
 * token-store directory, and error translation are split into small,
 * unit-tested collaborators.
 */
package dev.inboxpilot.infrastructure.gmail.auth;
