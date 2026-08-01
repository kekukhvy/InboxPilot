# InboxPilot design specification

## Purpose and safety model

InboxPilot inventories and analyzes a Gmail mailbox, proposes classifications,
and eventually applies explicit cleanup actions. Mailbox mutation is never an
implicit consequence of scanning or analysis: dry-run is the default, and only
an explicit execute step may change Gmail state.

Processing degrades per message where a usable partial result is possible. A
malformed message or a single provider rejection must be reported without
silently making an inventory look complete. Configuration and authorization
failures that prevent any useful work fail early with an actionable error.

## Architecture

InboxPilot uses hexagonal architecture within one Gradle module. Package
boundaries act as layers:

| Package | Responsibility | Allowed project dependencies |
|---|---|---|
| `dev.inboxpilot.domain` | Core model, invariants, and rules | none |
| `dev.inboxpilot.application` | Use cases and provider-independent ports | `domain` |
| `dev.inboxpilot.infrastructure` | Gmail, configuration, storage, and observability adapters | `application`, `domain` |
| `dev.inboxpilot.cli` | User entry points | `application`, `domain` |
| `dev.inboxpilot` | Composition root | all layers |

Dependencies point inward. Domain code contains no Spring, Google, Jackson, or
logging types. Google client types stay in infrastructure and are translated at
application-owned ports. These rules are enforced by `ArchitectureTest` and
explained in [ADR 0001](adr/0001-hexagonal-architecture.md).

## Error and observability model

Operations carry structured context such as operation name, message id,
attempt, elapsed time, outcome, and failure classification. Recoverable failures
are classified `TRANSIENT`; failures that require a configuration or data change
are `PERMANENT`. Domain remains logging-free, so adapters log failures where
they are caught, once, with the operation context attached.

Secrets, OAuth tokens, and credential payloads must never be logged.

## Gmail desktop authorization

The application owns the `CredentialProvider` port. It returns an immutable,
provider-independent `AccessToken`; the Google implementation lives in
`infrastructure.gmail.auth`. No caller outside infrastructure handles Google
credential types.

Authorization uses Google's installed-application authorization-code flow with
a local loopback receiver. The first authorization opens the system browser;
later authorizations reuse the refresh token stored by the Google client.

Before starting the browser flow, the adapter requires OAuth to be enabled and
requires non-blank client id and client secret values. Configuration, token
store, revoked-token, I/O, and transport-security failures are translated to
`CredentialProviderException` with an operator-actionable message. These are
permanent failures because retrying without changing configuration or
re-authorizing cannot resolve them.

The token store defaults to `~/.inboxpilot/tokens`, outside the source tree. Its
directory is created automatically and restricted to owner-only POSIX
permissions where supported. Filesystems without a POSIX permission view retain
their platform defaults and emit a warning; inability to create or secure the
directory is a permanent failure.

Configured scopes are requested exactly as supplied. The shipped configuration
contains only `gmail.readonly`. Broader scopes such as `gmail.modify` require an
explicit configuration change. The detailed rationale is recorded in
[ADR 0002](adr/0002-desktop-oauth-authorization.md).

## Persisted contracts

OAuth refresh-token persistence is owned by Google's file data-store
implementation and is not exposed as an InboxPilot model. Future InboxPilot
checkpoints, reports, or other serialized models are versioned contracts:
incompatible field changes must update their version marker and handle older
versions deliberately.

## Current limitations and deferred work

- The OAuth browser round-trip requires a real browser, loopback port, Google
  Cloud desktop client, and user consent; it is not exercised by hermetic unit
  tests.
- No real-mailbox verification target is configured, so live authorization is
  reported as not run because of the environment, never as passing.
- Wiring authorized credentials into the Gmail message gateway belongs to issue
  #10 and is outside the OAuth slice.
- Splitting package layers into separate Gradle modules remains deliberately
  deferred while ArchUnit provides the required boundary enforcement.
