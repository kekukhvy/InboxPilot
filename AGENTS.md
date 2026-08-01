# Project instructions for Codex

Codex follows the same development rules and project constraints as Claude in
this repository. The files under `.claude/` are the shared source of truth; the
directory name identifies the original tooling, not rules exclusive to Claude.

## Required reading

Before any non-trivial work, read:

1. `.claude/PROJECT.md` for the toolchain, commands, package layers, settled
   decisions, invariants, and Definition of Done.
2. `.claude/GUIDELINES.md` for the mandatory Clean Code and Clean Architecture
   rules.
3. `.claude/CLAUDE.md` for the repository workflow and source-document map.
4. The relevant section of the design specification named in
   `.claude/PROJECT.md` when changing the core model, rules, architecture, or a
   persisted format.

Do not guess project facts or duplicate them here. If the profile changes,
`.claude/PROJECT.md` remains the single source of truth for both Claude and
Codex.

## Mandatory development rules

- Preserve the package dependency direction declared in `.claude/PROJECT.md`.
  The domain layer stays framework-free and dependencies point inward.
- Follow every rule in `.claude/GUIDELINES.md`, including no inline magic
  literals, methods of at most 40 lines, SRP, KISS, DRY, intention-revealing
  names, guard clauses, and immutable value types where practical.
- Respect all settled decisions and invariants in `.claude/PROJECT.md`. Flag a
  contradiction instead of silently designing around or rewriting a settled
  decision.
- Treat persisted and serialized models as contracts. Handle incompatible
  format changes deliberately and update versioning when applicable.
- Use only the stack and exact build/test commands declared in
  `.claude/PROJECT.md`. Do not add a dependency when the current stack already
  covers the need; ask before introducing a genuinely necessary dependency.
- Use test-first development for feature and bug implementation. Cover happy
  paths, edge cases, and degradation/failure paths, preferring small real
  fixtures over mocks for structured input.
- Keep mailbox operations safe: dry-run remains the default, and mutation occurs
  only during an explicit execute step. Never expose secrets in source control
  or logs.
- Before finishing a code change, run the relevant tests and build required by
  the project profile and verify the checklist in `.claude/GUIDELINES.md`.

## Git and outward actions

- Follow the issue -> feature branch -> pull request -> merge workflow and the
  branch convention from `.claude/PROJECT.md`.
- Commit, push, or open a pull request only when the user explicitly asks.
- Never commit directly to the base branch or include build output, IDE files,
  secrets, or generated artifacts.

## Codex specialist agents

Specialist agents must also read this file plus `.claude/PROJECT.md` and
`.claude/GUIDELINES.md`. Their role-specific instructions narrow what they may
edit; they do not relax any project rule.
