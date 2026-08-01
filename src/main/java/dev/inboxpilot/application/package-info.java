/**
 * Use cases that orchestrate the domain.
 *
 * <p>Depends inward on {@link dev.inboxpilot.domain} only. Knows nothing about
 * Gmail, HTTP, or storage technology — those arrive through ports implemented in
 * {@link dev.inboxpilot.infrastructure}.
 */
package dev.inboxpilot.application;
