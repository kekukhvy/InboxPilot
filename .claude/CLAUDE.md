# Project Instructions for Claude

> Read this file first. The **project-specific facts** — name, repo, modules,
> build commands, settled decisions — live in [PROJECT.md](./PROJECT.md), which
> is the one file you edit when this `.claude/` directory is copied into a new
> project. Code rules live in [GUIDELINES.md](./GUIDELINES.md) and **must** be
> followed for every change.

---

## How this directory travels

`.claude/` is designed to be copied wholesale into another project. The split:

| File | Scope | Edit when copying? |
|---|---|---|
| `PROJECT.md` | **This project's** facts: name, repo, modules, commands, decisions | **Yes — always** |
| `CLAUDE.md` (this file) | How to work here; points at the others | Rarely |
| `GUIDELINES.md` | Coding rules (Clean Code / Clean Architecture) | Rarely |
| `README.md` | Map of the commands and agents | No |
| `commands/`, `agents/` | The SDLC pipeline | No |

Commands and agents read their project facts **from `PROJECT.md`**. If you find
a hardcoded project name, module, or build command in `commands/` or `agents/`,
that's a bug — move the value into `PROJECT.md` and reference it.

---

## Source documents (read before non-trivial work)

| File | What it covers |
|---|---|
| `.claude/PROJECT.md` | The project profile — modules, commands, invariants, settled decisions |
| `README.md` (repo root) | What the software is, how to run it |
| The design spec (path in `PROJECT.md`) | The *why*: core model, rules, formats, architecture, acceptance criteria |
| The per-slice specs dir | Per-slice specs written by `/specification` |

When a task touches the core model, the main rules, or a persisted format,
re-read the relevant section of the design spec rather than relying on memory —
decisions recorded there are deliberate and should not be re-litigated.

---

## Settled decisions & invariants

These live in [PROJECT.md](./PROJECT.md) under *Settled decisions* and
*Invariants*. Don't quietly design around them. If code contradicts one, **flag
it** rather than rewriting the rationale.

---

## Architecture

The module list and the dependency rule are in `PROJECT.md` (*Modules & layers*).
Two rules hold regardless of project:

- **Dependencies point one way.** The innermost layer (domain / core) depends on
  nothing else in the project. Outer layers depend inward only.
- **Stages and layers are independently testable.** Each has one responsibility
  and communicates through explicit models. A test that needs outer
  infrastructure to exercise inner logic is a sign the boundary leaked.

---

## Tech stack

Declared in `PROJECT.md` (*Toolchain*). Do not introduce a new framework,
library, or dependency to solve a problem the existing stack already covers. If
you believe a new dependency is genuinely required, say so and let the user
decide — don't add it silently.

---

## Build / run

Use the commands in `PROJECT.md` (*Toolchain*). Never guess a build invocation.

**Definition of Done** for a slice is defined in `PROJECT.md` — typically: the
acceptance criteria in the issue hold (proven by tests), the build is green, and
tests cover the edge cases and failure paths.

---

## Git workflow

- The base branch (per `PROJECT.md`) is stable. One feature branch per slice,
  branched from it, named per the **branch naming** convention.
- Flow: Issue → branch → PR → merge.
- Commit/push only when the user explicitly asks. Never commit directly to the
  base branch; branch first.
- Do **not** commit build output, IDE directories, or generated artifacts.

---

## AI SDLC — commands & subagents

This directory holds an issue → PR pipeline. The map of every command and agent
lives in [README.md](./README.md) (`/specification` → `/create-issue` →
`/implement` → `/review-cycle` → `/verify-coverage` → `/create-pr`, or `/ship`
for the whole slice).

Sync subagents keep artifacts aligned with the code:

- **spec-keeper** — updates the design spec when the model, formats, rules, or
  architecture change.
- **user-docs-writer** — updates the user-facing doc when flags, config, API, or
  output change.
- **test-author** — writes unit and fixture-based tests.
- **javadoc-writer** — API docs for public/protected types and methods.
- **logging-instrumenter** — logging at the right levels.
- **architecture-reviewer** — read-only review for Clean Architecture + Clean
  Code (layer boundaries, SRP, DRY, KISS, methods ≤40 lines). Guards against
  over-engineering — readability wins.

These are invoked **explicitly**, once per slice when the whole thing is ready to
sync — not per file edit. Each starts from a cold context, so re-running them
mid-slice is the single biggest driver of usage.

For code written by hand outside Claude, **`/sync`** diffs the changes and
delegates to the right subagents.

## When writing code

1. Read [PROJECT.md](./PROJECT.md) — modules, commands, invariants.
2. Follow [GUIDELINES.md](./GUIDELINES.md) — no exceptions.
3. Respect the dependency rule. The innermost layer imports nothing outward.
4. Re-read the relevant design-spec section for the area you touch.
5. Test: unit tests against small fixtures for logic; end-to-end runs for
   anything non-trivial.
