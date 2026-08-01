# ADR 0002 — Desktop OAuth authorization and credential-provider port

- **Status:** Accepted
- **Date:** 2026-08-01
- **Issue:** [#9](https://github.com/kekukhvy/InboxPilot/issues/9)

## Context

The application must authorize to read Gmail on behalf of a user at the desktop.
Google OAuth's _authorization code flow for installed applications_ is the
standard and secure pattern: the first run opens the user's browser at Google's
consent screen, the user grants permission, and the app receives a refresh token
it can persist and reuse later.

Two design concerns drive the specifics here:

1. **No Google types outside infrastructure.** ADR 0001 says "Gmail types stay in
   `infrastructure`" and the settled decision "Gmail types never cross the
   boundary" is enforced by `ArchitectureTest`. Use cases must never see a
   Google `Credential` object or learn how authorization was obtained — so they
   can be tested without Gmail, and so we can swap authorization methods later
   without rewriting the domain layer.

2. **Credential validation must happen early and translate to domain terms.**
   Invalid credentials (blank client id/secret, uncreatable token store, revoked
   refresh token) are permanent failures, not transient. The error messages must
   name the offending configuration key and environment variable, so an operator
   knows exactly what to fix — not a cryptic Google exception.

## Decision

### 1. The credential provider port

A new port interface `dev.inboxpilot.application.port.CredentialProvider`
declares how use cases obtain an access token:

```java
public interface CredentialProvider {
    AccessToken obtainAccessToken();   // throws CredentialProviderException (unchecked)
}
```

The port is a pure facade — expressed entirely in domain/JDK terms, never
Google types. Its only returned value is `AccessToken`, a plain record holding
the token string and its expiration instant.

**Why this boundary?** Use cases ask for an access token by calling the port;
infrastructure provides it by running Google's OAuth flow. Dependency inversion
applies — the application owns the interface, `infrastructure` implements it —
so the layer boundary is protected by the dependency rule and enforced by
`ArchitectureTest`.

### 2. The access token value type

`AccessToken` lives in `dev.inboxpilot.application.model`, not in `application.port`:

```java
public record AccessToken(String value, Instant expiresAt) {
    public boolean isExpiredAt(Instant instant) { /* ... */ }
}
```

**Why separate from the port?** `ArchitectureTest` requires every non-exception
type in `application.port` to be an interface (ADR 0001's "ports are interfaces"
rule). `AccessToken` is a concrete value type, so it lives in `application.model`
— a new package for JDK types the ports exchange, kept out of `application.port`
for this rule to hold.

### 3. Credential validation at the boundary

The adapter `GoogleCredentialProvider` translates any configuration or
authorization failure into a single exception type: `CredentialProviderException`.

Before the interactive browser flow runs, `OAuthCredentialGuard` checks:
- OAuth is enabled (`inboxpilot.oauth.enabled=true`)
- The client id is not blank
- The client secret is not blank

The guard translates `OAuthProperties`' validation errors (thrown as
`IllegalArgumentException`) into `CredentialProviderException`, so a caller
never catches two types.

**Why upfront validation?** Invalid credentials are permanent — retrying is
useless. Failing loudly before any network call saves time and confusion. The
error message names the config key and environment variable (e.g.,
`INBOXPILOT_OAUTH_CLIENT_ID`), so the operator knows exactly what to set.

### 4. Translating authorization failures

Google's OAuth client library raises various checked exceptions. `OAuthFailureTranslator`
recognizes the permanent failures:

- **Invalid grant** (`invalid_grant`): the refresh token was revoked or never issued.
  The message advises the user to delete the cached token and re-authorize.
- **IO failures** during the authorization exchange: generic, point at config verification.
- **Security exceptions** setting up the HTTPS transport: generic, advise config verification.

Every translated message names `inboxpilot.oauth.token-store`,
`inboxpilot.oauth.client-id`, or `inboxpilot.oauth.client-secret` and the
environment variable that supplies it, so the operator can act.

### 5. Token storage — long-lived secret, owner-only permissions

The refresh token is a long-lived credential: anyone who can read the file
can impersonate the user. `TokenStoreDirectory` prepares the configured directory
(`inboxpilot.oauth.token-store`, default `~/.inboxpilot/tokens`) and forces it
to POSIX owner-only permissions (`rwx------` / `0700`) where the filesystem
supports them.

On systems without POSIX permissions (Windows, FAT), permissions are left at the
platform default — a "degrade, don't fail" approach (project invariant). A
debug-level log message records this, so an operator knows the token store is
not owner-restricted.

### 6. Least-privilege scopes by default

The scope list requested at authorization is taken _exactly_ from the config
(`OAuthProperties.scopes()`), with no implicit additions. The default
configuration ships with `gmail.readonly` only:

```yaml
scopes:
  - https://www.googleapis.com/auth/gmail.readonly
```

This supports least privilege: the application can inventory and analyze
messages without the ability to modify. When cleanup (label/archive) ships
later, `gmail.modify` becomes an explicit configuration change, not a silent
default.

**Rationale:** Scope changes must be deliberate and documented, not hidden in
code. This also guards against the risk of accidentally shipping with broad
permissions.

### 7. Testability split

The interactive browser round-trip (`AuthorizationCodeInstalledApp` +
`LocalServerReceiver`) is kept to exactly two method calls in
`GoogleCredentialProvider.runAuthorizationFlow()`. Everything else is isolated
in unit-tested collaborators:

- `OAuthCredentialGuard` — validates configuration
- `TokenStoreDirectory` — prepares the directory and permissions
- `OAuthFailureTranslator` — translates exceptions

This split means the browser flow itself cannot be unit-tested (it requires a
real browser and local server), but the logic around it — validation, error
translation, permissions — is tested in isolation.

## Consequences

**We accept:**

- **Translation code.** Every Google OAuth failure is caught and re-wrapped in
  `CredentialProviderException`. That is the price of hiding the Google library
  behind a domain-language port.
- **Another port interface.** The application layer now has two ports
  (`CredentialProvider` and others to come). This is expected — ports are
  intended to scale as new external concerns arise.

**We get:**

- **Use cases never see Google OAuth types.** Classification and rule logic can
  be tested without an authorization token or a real Gmail account. The port
  seam makes it trivial to stub with a mock token for testing.
- **Configuration errors fail early with actionable messages.** The first call to
  `CredentialProvider` will tell the operator exactly which environment variable
  to set, not a cryptic Google exception 10 steps into the authorization flow.
- **Secrets stay out of the repository.** The refresh token is written to a
  filesystem directory outside the source tree, and its directory is restricted
  to the owner. The committed configuration files carry no secrets.
- **Scope changes are deliberate.** Adding `gmail.modify` requires editing the
  configuration file, making the change visible in reviews and CI logs. It
  cannot happen accidentally.

## Alternatives considered

- **Inject Google `Credential` directly into use cases.** Rejected: violates ADR
  0001's "Gmail types stay in infrastructure" rule, couples use cases to Google,
  and makes testing impossible without mocking the Google client library.
- **Persist secrets in the repository.** Rejected: violates the settled decision
  "secrets never reach the repository" and creates security risk on every clone.
- **Include `gmail.modify` in the default scope list.** Rejected: violates least
  privilege and makes it too easy to ship with broad permissions by accident.
  Scope expansion must be deliberate.
- **Fail the application if OAuth setup is incomplete.** Rejected: the
  application should be runnable before Gmail is configured, so early
  development and testing can happen on the inventory and analysis logic
  without requiring an OAuth setup. Validation only happens when `enabled=true`.

## Related decisions

- **ADR 0001:** Hexagonal architecture and package boundaries. This ADR applies
  the port-and-adapter pattern to the new OAuth domain.
- **Settled decision 3** (`PROJECT.md`): "Gmail types stay in `infrastructure`."
  This ADR documents how that rule is upheld for OAuth — the
  `CredentialProvider` port and `AccessToken` value type form the boundary.
- **Invariant:** "Secrets never reach the repository." Implemented here by
  persisting the refresh token outside the source tree and enforcing
  owner-only permissions.
