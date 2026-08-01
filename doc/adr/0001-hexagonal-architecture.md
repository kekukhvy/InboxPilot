# ADR 0001 — Hexagonal architecture and package boundaries

- **Status:** Accepted
- **Date:** 2026-08-01
- **Issue:** [#3](https://github.com/kekukhvy/InboxPilot/issues/3)

## Context

InboxPilot reads a Gmail mailbox, decides what each message means, and applies
labels or cleanup actions. Two properties drive the design:

1. **The decisions are the valuable part.** Whether a message is a stale
   newsletter, whether two labels conflict, whether a rule matches — that logic
   must be testable on its own, without a network, an OAuth token, or a mailbox.
2. **Gmail is a detail that will change.** The Gmail API is one source of
   messages. Its client types (`com.google.api.services.gmail.model.Message`,
   `Label`, …) carry API-shaped concerns — nullable fields, pagination tokens,
   internal date strings — that have nothing to do with what a message *means*.

If Gmail types reach the classification logic, every rule test needs a Gmail
fixture, and swapping or mocking the source becomes a rewrite. That is the
failure this ADR prevents.

## Decision

We adopt a **hexagonal (ports and adapters)** architecture, enforced by package
boundaries inside the single `inboxpilot` Gradle module.

### The layers

| Package | Role | May depend on |
|---|---|---|
| `dev.inboxpilot.domain` | Core model and rules | *Nothing in the project* |
| `dev.inboxpilot.application` | Use cases; declares **ports** | `domain` |
| `dev.inboxpilot.infrastructure` | **Adapters**: Gmail, storage, files | `domain`, `application` |
| `dev.inboxpilot.cli` | Command-line entry points | `domain`, `application` |
| `dev.inboxpilot` (root) | Composition root | all of the above |

**Dependencies point inward only.** `domain` is the centre and depends on
nothing — not on `application`, not on Spring, not on Gmail.

### Ports and adapters

A **port** is an interface declared in `application`, expressed purely in domain
terms:

```java
package dev.inboxpilot.application.port;

public interface MessageSource {
    List<MailMessage> fetchSince(Instant since);   // MailMessage is a domain type
}
```

An **adapter** in `infrastructure` implements that port and is the *only* place
that knows about Gmail:

```java
package dev.inboxpilot.infrastructure.gmail;

class GmailMessageSource implements MessageSource {
    // Google client types live here and are translated to domain types.
}
```

Dependency inversion applies: `application` owns the interface, `infrastructure`
depends on `application` to implement it. At runtime the composition root injects
the adapter, so the direction of *control* flows outward while the direction of
*source dependency* flows inward.

### The framework-free rule

`domain` contains **no framework types at all** — no Spring annotations, no
Jackson annotations, no logging. Two consequences:

- Domain rules are tested with plain JUnit; no Spring context, no mailbox.
- Logging belongs where domain exceptions are *caught* (`application`,
  `infrastructure`), never inside `domain`. This is why
  `.claude/PROJECT.md` sets the logging exclusion to `domain`.

## Enforcement

Documentation alone decays. `ArchitectureTest` (ArchUnit) fails the build on
violation, checking:

1. **Layered dependencies** — `domain` is not accessed by anything it must not
   know about, and each layer only depends inward.
2. **`domain` is framework-free** — no Spring, Jackson, SLF4J, or Google API
   types reachable from `domain`.
3. **Gmail types do not leak** — `com.google...` types appear only in
   `infrastructure`. This is AC3 of issue #3, checked mechanically.
4. **No cycles** between the layer packages.

The test runs as part of `./gradlew test`, so a boundary violation breaks the
build rather than surviving review.

## Consequences

**We accept:**

- **Translation code.** Every Gmail type must be mapped to a domain type in the
  adapter. That mapping is real work and is the price of the boundary.
- **More indirection for simple flows.** Reading a message goes through a port
  rather than calling the Gmail client directly.

**We get:**

- Classification and rule logic testable with plain objects — fast, hermetic, no
  network.
- Gmail replaceable or mockable at one seam.
- A boundary that is *checked*, not merely documented.

**Deliberately deferred:** splitting the layers into separate Gradle modules.
Package boundaries plus ArchUnit give the same protection at a fraction of the
build complexity. If the module count grows enough that packages stop being
convincing, this decision can be revisited without changing any layer's code.

## Alternatives considered

- **Plain layered architecture (`controller` → `service` → `repository`).**
  Rejected: it does not invert the infrastructure dependency, so Gmail types
  reach the service layer and the rules can no longer be tested in isolation.
- **Separate Gradle modules per layer now.** Rejected as premature for a
  single-deployable CLI — see *deferred* above.
- **Documentation without ArchUnit.** Rejected: unenforced boundaries erode, and
  AC2/AC3 of issue #3 ask for boundaries that actually hold.
