/**
 * Value types the application ports speak in.
 *
 * <p>Kept separate from {@link dev.inboxpilot.application.port} because the
 * port package is a fixed convention — every non-exception type there must be
 * an interface (ADR 0001, enforced by {@code ArchitectureTest}) so it can be
 * stubbed or swapped. These are the plain values ports accept and return.
 */
package dev.inboxpilot.application.model;
