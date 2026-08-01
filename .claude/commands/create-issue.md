---
description: Create a well-formed GitHub issue from a specification file, with the right labels and milestone
---

Turn a specification into a GitHub issue in this project's repo, with all
metadata (labels, milestone) set. `$ARGUMENTS` is expected to be a path to a spec
file produced by `/specification` (e.g. `doc/specs/<slug>.md`). Use the `gh` CLI
to create the issue.

The discussion already happened in `/specification` — **do not re-open it**. This
command reads the finished spec, classifies it, and creates the issue.

## Step 0 — Load the project profile

Read `.claude/PROJECT.md`. Take from it:

- **GitHub repo** — the only repo you may create an issue in
- **Label taxonomy** and **path → area mapping** — the fallback classification
- **Per-slice specs** directory — where to look for spec files
- **Definition of Done** — folded into acceptance criteria

Never use a repo, label, or milestone that isn't confirmed by the profile *and*
the live repo (Step 2).

## Ground rules

- **The spec is the input.** Read it and build the issue from it. Only ask the
  user something if the spec is missing or genuinely ambiguous for
  classification — don't re-litigate the design.
- Never invent a label or milestone. Only use ones that actually exist in the
  repo (fetched in Step 2). If unsure a label fits, leave it off rather than
  guess.
- Create issues only in the repo named in `PROJECT.md`. Nowhere else.

## Step 1 — Read the specification

Resolve `$ARGUMENTS`:

- If it's a path to an existing file (typically under the per-slice specs
  directory), read it.
- If it's empty, look in that directory and ask the user which spec to use (list
  the candidates). If there are none, tell the user to run `/specification` first.
- If it's free-form text rather than a path, treat it as an inline spec, but note
  to the user that running `/specification` first produces a better issue.

The spec's **Issue metadata (suggested)** block, if present, is a hand-off hint —
use it as a starting point but still validate it in Steps 2–3 against the live
repo. Everything else in the spec feeds the issue title and body.

## Step 2 — Refresh the metadata (don't trust a stale list)

Labels and milestones change over time. **The live repo wins over `PROJECT.md`.**
Fetch the current sets first, substituting the repo from the profile:

```bash
gh label list --limit 100
gh api repos/<repo>/milestones --jq '.[] | "\(.number)\t\(.title)\t(\(.state))"'
```

If a label in `PROJECT.md` no longer exists in the repo, use the live one and
mention the drift so the profile can be corrected.

## Step 3 — Classify the issue

Using the spec (its metadata block and content) plus repo context, decide:

1. **Type** — exactly one, from the type labels in the live list.
2. **Area / module** — which part of the codebase does it touch? Use the
   **path → area mapping** in `PROJECT.md` to translate the affected code paths
   into labels. Add more than one only if the work genuinely spans areas.
3. **Priority** — exactly one. Default to the middle priority unless the
   description signals otherwise (a broken production path / data-loss risk →
   highest; nice-to-have / cosmetic → lowest).
4. **Milestone** — map the work to the matching roadmap milestone. Pick the
   single best-fit open one. If nothing fits, leave the milestone unset and say so.
5. **Status** — add a status label (e.g. `needs-design`) only if the repo has one
   and the spec leaves design questions genuinely open.

If a classification is still ambiguous from the spec, ask one focused question
rather than guessing.

## Step 4 — Write the issue body

Turn the spec into the issue body. Keep the title short and imperative. Map the
spec's sections onto the issue body — Problem/Design → Summary + Context, and the
spec's Acceptance criteria carry over directly. Structure the body with these
sections (omit a section only if truly N/A):

```markdown
## Summary
<one or two sentences on what and why>

## Context
<relevant background: which area, what exists today, why this is needed>

## Acceptance criteria
- [ ] <verifiable outcome 1>
- [ ] <verifiable outcome 2>

## Notes
<optional: constraints, links to design-spec sections, related issues>
<when built from a spec file, link it, e.g. "Spec: doc/specs/<slug>.md">
```

Fold the project's **Definition of Done** (from `PROJECT.md`) into the acceptance
criteria where the work warrants it.

## Step 5 — Confirm, then create

Show the user the final title, body, labels, and milestone. Create the issue only
after they approve. Pass each label with its own `--label` flag and the milestone
by title:

```bash
gh issue create \
  --repo <repo> \
  --title "<title>" \
  --body "<body>" \
  --label "<type>" --label "<area>" --label "<priority>" \
  --milestone "<Milestone Title>"
```

Report back the created issue URL. If `gh issue create` fails (e.g. a label was
renamed), read the error, correct the offending flag against the live lists from
Step 2, and retry — do not silently drop metadata.
