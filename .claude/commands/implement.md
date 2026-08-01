---
description: Implement an issue with TDD, then sync spec/docs/logging/API-docs — from an issue number to tested, documented code on a feature branch
---

Take a GitHub issue and produce **tested, documented** code for it on a feature
branch, test-first. This is the build stage of the SDLC: it drives implementation
via TDD and then brings the surrounding artifacts (spec, user docs, logging, API
docs, tests) back in sync. It does **not** review, fix, verify acceptance, or
open a PR — those are later stages (`/review-cycle`, `/verify-coverage`,
`/create-pr`), tied together by `/ship`.

`$ARGUMENTS` — the issue: a number (`31` or `#31`). Optionally a spec path to use
as the source of acceptance criteria. If empty, ask which issue to implement.

## Step 0 — Load the project profile

Read `.claude/PROJECT.md`. Take from it: the **base branch**, the **branch
naming** convention, the **build/test commands**, the **modules and dependency
rule**, the **design spec** and **per-slice specs** paths, and the **Definition
of Done**. Every command you run below comes from this profile — never hardcode
one.

## Step 1 — Load the issue and its acceptance criteria

```bash
gh issue view <n> --json title,body,labels,milestone
git branch --show-current
```

- Read the issue body; locate the linked spec in the per-slice specs directory
  (issues created by `/create-issue` reference one) and read its
  `## Acceptance criteria`.
- If neither issue nor spec yields acceptance criteria, stop and ask — TDD needs
  a concrete target.

## Step 2 — Create the feature branch

Per the git workflow (CLAUDE.md): branch from the base branch, one branch per
slice, named per the **branch naming** convention in `PROJECT.md`.

```bash
git switch <base> && git pull      # start from an up-to-date base
git switch -c <issue-number>-<slug>
```

If already on a matching feature branch for this issue, stay on it. Never
implement on the base branch — branch first.

## Step 3 — Implement with TDD (delegate)

Invoke the **tdd-implementer** subagent via the Task tool, passing the issue
number, the spec path, and the numbered acceptance criteria. It runs strict
red → green → refactor cycles (innermost layer outward), honoring the layer +
Clean Code rules, and writes both tests and production code. Wait for it to
report the cycles completed and the final build result.

If it reports a criterion it could not satisfy, surface that — don't paper over
it. Decide with the user whether to adjust scope or continue.

## Step 4 — Sync the surrounding artifacts

Now that code exists, bring the rest in sync. First establish the **scope** — you
will need it either way:

```bash
git diff --stat <base>...HEAD      # which files, how many lines
git diff <base>...HEAD             # the actual change
```

### Small change → do it yourself, no subagents

If the change is small (rule of thumb: **≤ 3 files or ≤ ~50 changed lines**),
write the API docs, logging, and any missing tests **directly**, in this context
— you have already read the diff. A subagent starts from a **cold context**: it
re-reads the project from scratch, so three cold starts to service a 20-line diff
cost far more than the work itself. Delegate only when the change is big enough
that the re-reading pays for itself.

### Larger change → delegate, but hand over the scope

Run the applicable subagents in parallel via the Task tool. Decide which ones the
change actually calls for and skip the rest — a UI-only slice needs no
`spec-keeper`; a change confined to the framework-free inner layer needs no
`logging-instrumenter`.

- **spec-keeper** — if the domain model, core rules, a serialised/persisted
  format, or the architecture changed → updates the design spec.
- **user-docs-writer** — if flags, config keys, endpoints, or other user-facing
  behavior changed → updates the user-facing doc.
- **test-author** — to backfill edge-case tests the TDD cycles didn't cover (TDD
  covers the driven behaviors; test-author widens coverage).
- **javadoc-writer** — API docs for new/changed public/protected types & members.
- **logging-instrumenter** — logging at the right levels for new code outside the
  framework-free layer (which it never touches).

**Put the scope in the prompt.** Each subagent's prompt must carry the concrete
list of changed files and the diff (or the relevant slice of it) — not just the
issue number and a topic. A subagent inherits **none** of this conversation, so
without it, it burns its first dozen tool calls rediscovering what you already
know (`git status`, `git diff`, walking the tree). Hand it the facts; it will
still read the code itself, so accuracy does not depend on your summary.

This is the **only** place the sync agents run during the pipeline — once, for
the whole slice, never per file edit. `/sync` covers hand-written code separately.

**Record a run marker for every sync agent.** After running (or deliberately
skipping) each of the five sync agents, append one line to
`.claude/sdlc/<branch-slug>.md` (create the dir/file if absent) so the pre-PR
`/sdlc-check` gate can tell "not needed" from "forgotten":

```
- <agent-name> | <ISO-date> | ran: <one-line what it did> | files: <n>
- <agent-name> | <ISO-date> | skipped: <why not needed for this diff>
```

Mark a `skipped:` line only when the diff genuinely doesn't trigger that agent
(per the table above) — the gate re-checks your call against the diff, so an
untruthful skip will be caught and block the PR.

## Step 5 — Build green and report

Run the **module build command** from `PROJECT.md` for the affected module, or
the full **build command** for cross-module changes.

Report: the branch, acceptance criteria and their test status, cycles run, files
added/changed, which sync subagents ran and what they updated, and the final
build result. If the build is red, say so with the output — do not claim done.

Point the user at the next stage: `/review-cycle` (review→validate→fix) then
`/verify-coverage` and `/create-pr`, or `/ship` to run the whole remaining
pipeline. Do not commit or push here unless the user asks.
