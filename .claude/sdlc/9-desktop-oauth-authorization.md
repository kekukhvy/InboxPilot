# SDLC run record — issue #9 (Desktop OAuth authorization for Gmail)

Branch `9-desktop-oauth-authorization`, slice `main..HEAD`.

- tdd-implementer | 2026-08-01 | ran: 9 red→green→refactor cycles — AccessToken, CredentialProvider(+Exception), OAuthCredentialGuard, TokenStoreDirectory, OAuthFailureTranslator, GoogleCredentialProvider, and the narrowed default scope | files: 16
- spec-keeper | 2026-08-01 | ran: created the canonical `doc/specification.md` and wrote `doc/adr/0002-desktop-oauth-authorization.md`, recording the credential-provider port, the new `application.model` package, least-privilege scopes, token-store secrecy, and permanent-failure translation | files: 2
- user-docs-writer | 2026-08-01 | ran: corrected the stale `scopes` default row in README.md and added an "Authorization flow and first-run setup" section (Google Cloud prerequisite, browser consent, token cache, scope escalation, the five permanent error messages) | files: 1
- test-author | 2026-08-01 | ran: backfilled port-boundary, invalid-grant/security translation, token-directory creation, and POSIX permission edge cases | files: 3
- javadoc-writer | 2026-08-01 | skipped: every new public/protected type and member already carries substantive Javadoc written during the TDD cycles (AccessToken, CredentialProvider, CredentialProviderException, plus package-info for the two new packages); nothing was left undocumented for it to add
- logging-instrumenter | 2026-08-01 | ran: added operation-scoped success/failure and consent-flow logs to `GoogleCredentialProvider`, plus token-store preparation, permission, and degraded-filesystem logs to `TokenStoreDirectory` | files: 2

## Verification

- Acceptance criteria: least-privilege scope covered by `OAuthScopesTest`; token
  storage location and permissions covered by `TokenStoreDirectoryTest`; missing
  and invalid credentials covered by `OAuthCredentialGuardTest`,
  `OAuthFailureTranslatorTest`, and `GoogleCredentialProviderTest`.
- `./gradlew build` — PASS (2026-08-01).
- Live Google authorization — not-run (environment): the verification project
  in `.claude/PROJECT.md` is `none` and no test account is configured.

## Dependency decision

This slice needed Google's OAuth client libraries, which the stack did not
cover. Per CLAUDE.md ("do not introduce a new dependency silently"), this was
put to the user, who approved adding them:

| Coordinate | Version |
|---|---|
| `com.google.oauth-client:google-oauth-client-jetty` | 1.39.0 |
| `com.google.api-client:google-api-client` | 2.9.0 |

Versions are pinned in `gradle/libs.versions.toml`; `build.gradle.kts` uses only
`libs.*` aliases. The types are confined to `infrastructure.gmail.auth`, which
`ArchitectureTest.doNotLeakOutsideInfrastructure` enforces.

## Scope note

Issue #9 is OAuth only. Wiring the `CredentialProvider` into `GmailMessageSource`
belongs to issue #10 (the gateway) and was deliberately left out —
`GmailMessageSource` still fails loudly as "not implemented", and its test is
untouched.

## Known limits

- The interactive browser round-trip (`AuthorizationCodeInstalledApp.authorize`)
  cannot run in a unit test. It is kept to two calls at the bottom of the call
  chain; everything around it — configuration guarding, token-store preparation,
  scope resolution, failure translation — is unit-tested in isolation.
- No end-to-end test against a real Google account: `PROJECT.md` sets the
  verification project to `none`, so that criterion is **not-run (environment)**,
  not a pass.
