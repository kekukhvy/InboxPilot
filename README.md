# InboxPilot

Safe, rule-driven Gmail mailbox triage, classification, and cleanup.

InboxPilot inventories a Gmail mailbox, proposes a label taxonomy from what it
finds, and applies declarative YAML rules to classify and clean up mail — with a
dry run first, so nothing changes in your mailbox until you say so.

> **Status:** early foundation. This repository currently contains the project
> skeleton (issue #2). Gmail integration, the rule engine, and classification
> land in later slices — see the [open issues](https://github.com/kekukhvy/InboxPilot/issues).

---

## Requirements

- **Java 21 or newer.** The Gradle toolchain pins the language level to Java 21,
  so a newer JDK on your machine is fine — Gradle selects or downloads a
  matching toolchain.
- No local Gradle install needed — use the wrapper (`./gradlew`).

Check your JDK:

```bash
java -version
```

---

## Build

Compile and run the full test suite:

```bash
./gradlew clean test
```

Build everything, including the executable jar:

```bash
./gradlew build
```

The jar is written to `build/libs/inboxpilot-0.1.0-SNAPSHOT.jar`.

---

## Run

During development, run straight from Gradle:

```bash
./gradlew bootRun
```

Or run the packaged jar:

```bash
./gradlew build
java -jar build/libs/inboxpilot-0.1.0-SNAPSHOT.jar
```

InboxPilot runs as a short-lived process rather than a server: the application
context starts, does its work, and exits.

---

## Configuration

Defaults live in `src/main/resources/application.yml`, and
[`config/application-example.yml`](config/application-example.yml) is a fully
commented template. Override without editing either — Spring Boot reads, in
increasing precedence: the bundled `application.yml`, a config file you point it
at, environment variables, then command-line arguments.

**Every setting is validated at startup.** A missing, malformed, or out-of-range
value stops the application with a message naming the offending key, rather than
failing later against a real mailbox.

### Getting started

```bash
cp config/application-example.yml config/application-local.yml
# edit config/application-local.yml, then:
java -jar build/libs/inboxpilot-0.1.0-SNAPSHOT.jar \
     --spring.config.additional-location=file:./config/application-local.yml
```

`config/application-local.yml` is git-ignored. See **Secrets** below.

### Settings

All keys are under the `inboxpilot.` prefix.

**OAuth** (`inboxpilot.oauth.*`) — Google credentials; see issue #9

| Key | Meaning | Default |
|---|---|---|
| `enabled` | Whether Gmail access is configured. Credentials are validated only when `true`, so the app runs before OAuth is set up. | `false` |
| `client-id` | OAuth client id from Google Cloud | from `INBOXPILOT_OAUTH_CLIENT_ID` |
| `client-secret` | OAuth client secret — **never commit** | from `INBOXPILOT_OAUTH_CLIENT_SECRET` |
| `token-store` | Directory caching the refresh token | `~/.inboxpilot/tokens` |
| `scopes` | Gmail scopes requested; at least one | readonly + modify |

**Scanning** (`inboxpilot.scanning.*`) — what a scan covers

| Key | Meaning | Default |
|---|---|---|
| `lookback` | How far back to scan (`365d`, `12h`, …) | `365d` |
| `max-messages` | Ceiling for one run; `1`–`1000000` | `50000` |
| `include-spam` | Include the spam folder | `false` |
| `include-trash` | Include the trash folder | `false` |

**Batching** (`inboxpilot.batching.*`) — request batching and retry

| Key | Meaning | Default |
|---|---|---|
| `batch-size` | Messages per batch call; `1`–`100` (Gmail's limit) | `100` |
| `max-retries` | Retries after a retryable failure; `0`–`10` | `5` |
| `initial-backoff` | First wait before retrying; doubles per attempt | `1s` |
| `max-backoff` | Ceiling on backoff; must be ≥ `initial-backoff` | `60s` |

**Reports** (`inboxpilot.reports.*`)

| Key | Meaning | Default |
|---|---|---|
| `output-directory` | Where reports are written | `./reports` |
| `formats` | One or more of `JSON`, `CSV`, `HTML` | `JSON`, `HTML` |
| `overwrite` | Whether an existing report may be replaced | `false` |

**Checkpoints** (`inboxpilot.checkpoints.*`) — resumable progress

| Key | Meaning | Default |
|---|---|---|
| `enabled` | Checkpoint progress so a failed run resumes | `true` |
| `file` | File holding checkpoint state | `./.inboxpilot/checkpoint.json` |
| `interval` | Messages between checkpoint writes; ≥ `1` | `500` |

**Logging**

| Key | Meaning | Default |
|---|---|---|
| `logging.level.root` | Log level for everything | `INFO` |
| `logging.level.dev.inboxpilot` | Log level for InboxPilot's own code | `INFO` |

### Secrets

OAuth credentials must never reach the repository. Supply them through the
environment:

```bash
export INBOXPILOT_OAUTH_CLIENT_ID='...'
export INBOXPILOT_OAUTH_CLIENT_SECRET='...'
export INBOXPILOT_OAUTH_ENABLED=true
```

`.gitignore` blocks `config/*.yml` (except the example), `credentials.json`,
`client_secret*.json`, `tokens/`, `.inboxpilot/`, and `.env*`. If you add a new
kind of secret file, add it there too.

### Examples

Turn on debug logging for one run:

```bash
./gradlew bootRun --args="--logging.level.dev.inboxpilot=DEBUG"
```

Scan only the last 30 days, in smaller batches:

```bash
java -jar build/libs/inboxpilot-0.1.0-SNAPSHOT.jar \
     --inboxpilot.scanning.lookback=30d \
     --inboxpilot.batching.batch-size=25
```

---

## Project layout

A single Gradle module; boundaries are enforced by package, with dependencies
pointing inward (see issue #3 for the architecture decision record).

```
src/main/java/dev/inboxpilot/
    InboxPilotApplication.java   # composition root and entry point
    domain/                      # core model and rules — no framework types
    application/                 # use cases orchestrating the domain
    infrastructure/              # Gmail, storage, and other outbound adapters
    cli/                         # command-line entry points
```

Dependency versions are centralized in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml); `build.gradle.kts`
contains no version literals.

---

## Development

The repository carries an AI-assisted SDLC pipeline under `.claude/` (issue →
spec → TDD → review → PR). See [`.claude/README.md`](.claude/README.md) for the
commands and agents, and [`.claude/GUIDELINES.md`](.claude/GUIDELINES.md) for
the coding rules every change must follow.
