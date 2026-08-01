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

Settings live in `src/main/resources/application.yml`. Override any of them
without editing the file — Spring Boot reads, in increasing precedence: the
bundled `application.yml`, an `application.yml` next to the jar, environment
variables, then command-line arguments.

| Key | Meaning | Default |
|---|---|---|
| `spring.application.name` | Application name shown in logs | `inboxpilot` |
| `spring.main.web-application-type` | Runtime style; `none` = no web server | `none` |
| `logging.level.root` | Log level for everything | `INFO` |
| `logging.level.dev.inboxpilot` | Log level for InboxPilot's own code | `INFO` |

Examples — turn on debug logging for one run:

```bash
./gradlew bootRun --args="--logging.level.dev.inboxpilot=DEBUG"
```

```bash
java -jar build/libs/inboxpilot-0.1.0-SNAPSHOT.jar --logging.level.dev.inboxpilot=DEBUG
```

---

## Architecture

InboxPilot uses a **hexagonal (ports and adapters)** architecture — see
[ADR 0001](doc/adr/0001-hexagonal-architecture.md) for the reasoning. A single
Gradle module; boundaries are enforced by package, with dependencies pointing
inward.

```
src/main/java/dev/inboxpilot/
    InboxPilotApplication.java   # composition root and entry point
    domain/                      # core model and rules — no framework types
    application/                 # use cases; declares ports
    infrastructure/              # adapters: Gmail, storage, files
    cli/                         # command-line entry points
```

| Layer | May depend on |
|---|---|
| `domain` | *nothing in the project* |
| `application` | `domain` |
| `infrastructure` | `domain`, `application` |
| `cli` | `domain`, `application` |

Two consequences worth knowing before you add code:

- **`domain` is plain Java.** No Spring, Jackson, SLF4J, or Gmail types. Domain
  rules are tested with plain JUnit — no Spring context, no mailbox.
- **Gmail types live only in `infrastructure`.** Everything else speaks in domain
  terms, via ports declared in `application`.

These are not honour-system rules: `ArchitectureTest` fails the build if a
boundary is crossed, so a violation shows up in `./gradlew test` rather than in
review.

Dependency versions are centralized in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml); `build.gradle.kts`
contains no version literals.

---

## Development

The repository carries an AI-assisted SDLC pipeline under `.claude/` (issue →
spec → TDD → review → PR). See [`.claude/README.md`](.claude/README.md) for the
commands and agents, and [`.claude/GUIDELINES.md`](.claude/GUIDELINES.md) for
the coding rules every change must follow.
