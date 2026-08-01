# Project profile

> **This is the only file you edit when you copy `.claude/` into a new project.**
> Every command and agent reads its project-specific facts from here. If a value
> below is wrong, the whole pipeline is wrong — fill it in before the first run.

Fill in each field. Where a field genuinely does not apply, write `none` rather
than deleting the row — agents check for the key.

---

## Identity

| Key | Value |
|---|---|
| **Project name** | InboxPilot |
| **One-line purpose** | Safe, rule-driven Gmail mailbox triage, classification, and cleanup |
| **GitHub repo** | `kekukhvy/InboxPilot` |
| **Base branch** | `main` |
| **Package root** | `dev.inboxpilot` |
| **Branch naming** | `<issue-number>-<slug>` |

## Toolchain

| Key | Value |
|---|---|
| **Language / version** | Java 21 |
| **Build tool** | Gradle (Kotlin DSL) |
| **Build command** | `./gradlew build` |
| **Test command** | `./gradlew test` |
| **Module test command** | `./gradlew test --tests '<FQCN>'` (single module) |
| **Module build command** | `./gradlew build` (single module) |
| **Compile command** | `./gradlew compileJava` |
| **Run command** | `./gradlew bootRun` |
| **Framework** | Spring Boot 4.1.0 |
| **Test framework** | JUnit 5 |
| **Test results (XML)** | `<module>/build/test-results/test/*.xml` |
| **Test report (HTML)** | `<module>/build/reports/tests/test/index.html` |
| **Logging framework** | SLF4J + Logback |
| **API doc style** | Javadoc |
| **Test source root** | `**/src/test/**` |
| **Main source root** | `**/src/main/**` |

## Modules & layers

List the build modules and the dependency direction. Agents enforce this.

Single Gradle module (`inboxpilot`); boundaries are enforced by **package**, not
by separate build modules. Splitting into Gradle modules is deliberately deferred
— see issue #3.

| Package | Responsibility |
|---|---|
| `dev.inboxpilot` | Composition root (`InboxPilotApplication`) |
| `dev.inboxpilot.domain` | Core model and rules — no framework types |
| `dev.inboxpilot.application` | Use cases orchestrating the domain |
| `dev.inboxpilot.infrastructure` | Gmail, storage, and other outbound adapters |
| `dev.inboxpilot.cli` | Command-line entry points |

**Dependency rule:** `cli` → `application` → `domain`; `infrastructure` →
`application` → `domain`. **`domain` depends on nothing.** Dependencies point
inward only.

**Layer names used in this project:** `domain`, `application`, `infrastructure`,
`cli`

**Logging exclusion:** never add logging to `domain` — it must stay
framework-free. _(Set to `none` if the project has no such layer.)_

## Documents

| Key | Path |
|---|---|
| **Design spec (the *why*)** | `doc/specification.md` |
| **Per-slice specs** | `doc/specs/` |
| **User-facing docs** | `README.md` |
| **Coding rules** | `.claude/GUIDELINES.md` |
| **Project instructions** | `.claude/CLAUDE.md` |
| **User config file** | `src/main/resources/application.yml` |

## Settled decisions

Decisions that are **deliberate and not to be re-litigated** by any agent. The
`spec-keeper` and `architecture-reviewer` treat contradicting code as a finding
to *flag*, not as license to rewrite the rationale.

1. _(fill in — e.g. "Dry-run is the default; nothing mutates the mailbox without
   an explicit execute flag")_
2. _(…)_

If this project has no settled decisions yet, write `none yet` — agents will
skip the check.

## Invariants

Hard rules a change must never break. Agents check these during review.

- _(e.g. "Never mutate the mailbox outside an explicit execute step")_
- _(e.g. "Every destructive action is reversible via an exported rollback plan")_

## Definition of Done

What "done" means for a slice in this project. Folded into acceptance criteria
by `/specification`, and checked by `acceptance-verifier`.

- The acceptance criteria in the issue hold, proven by tests
- `./gradlew build` is green
- Tests cover the edge cases and the failure paths
- _(add project-specific gates, e.g. "runs end-to-end against a real mailbox in
  dry-run mode without errors")_

## Verification project

An optional real codebase to run end-to-end checks against, larger than the unit
fixtures.

| Key | Value |
|---|---|
| **Path** | `none` |
| **What it exercises** | — |

When the path is `none` or absent, end-to-end criteria are reported as
**not-run (environment)** — never as a pass.

## GitHub metadata

The live repo is always the source of truth — agents re-fetch with
`gh label list` and `gh api repos/<repo>/milestones` before using these. The
lists below are the fallback and the intent.

**Label taxonomy**

- **Type** (exactly one): `type: feature`, `type: bug`, `type: chore`,
  `type: test`, `type: documentation`, `type: epic`, `type: enhancement`
- **Area** (one or more): `area: foundation`, `area: gmail`, `area: inventory`,
  `area: analysis`, `area: rules`, `area: classification`, `area: cleanup`,
  `area: ai`, `area: ui`
- **Priority** (exactly one): `priority: critical`, `priority: high`,
  `priority: medium`, `priority: low`
- **Status** (optional): `status: needs-design`

**Path → area mapping** — used to classify an issue or PR from the diff:

| Changed path | Area label |
|---|---|
| _(fill in once modules exist, e.g. `**/gmail/**` → `area: gmail`)_ | |

**Milestones:** `v0.1 Foundation`, and the roadmap milestones in the repo.
