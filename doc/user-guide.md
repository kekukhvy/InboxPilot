# InboxPilot user guide

InboxPilot analyzes Gmail metadata, produces deterministic reports and rule
plans, and keeps every mailbox mutation behind an explicit execute step. This
guide covers installation, OAuth, configuration, inventory, analysis, rules,
classification, cleanup, recovery, and troubleshooting.

## Install and run

Requirements: Java 21 or newer. The repository includes the Gradle wrapper.

```bash
./gradlew build
java -jar build/libs/inboxpilot-0.1.0-SNAPSHOT.jar
```

During development, replace the `java -jar` invocation with:

```bash
./gradlew bootRun --args='<command>'
```

The currently exposed CLI commands are:

```text
labels list
messages list --query=<gmail-query>
checkpoint reset
```

`labels list` prints Gmail label ID and visible name separated by a tab.
`messages list` accepts normal Gmail search syntax. Quote the complete Gradle
argument when a query contains spaces.

## OAuth setup

1. In Google Cloud Console, enable the Gmail API.
2. Create an OAuth client with application type **Desktop app**.
3. Export the client credentials and enable authorization:

   ```bash
   export INBOXPILOT_OAUTH_ENABLED=true
   export INBOXPILOT_OAUTH_CLIENT_ID='<client-id>'
   export INBOXPILOT_OAUTH_CLIENT_SECRET='<client-secret>'
   ```

4. Run a read command such as `labels list`.
5. Approve the browser consent screen. The refresh token is cached under
   `~/.inboxpilot/tokens` by default.

The shipped scope is `gmail.readonly`. Inventory, analysis, dry-run, and report
generation work with read-only access. Label and archive execution additionally
require this deliberate configuration:

```yaml
inboxpilot:
  oauth:
    scopes:
      - https://www.googleapis.com/auth/gmail.readonly
      - https://www.googleapis.com/auth/gmail.modify
```

Changing scopes may require deleting the cached token and authorizing again.

## Configuration

Copy the commented template and keep the local file untracked:

```bash
cp config/application-example.yml config/application-local.yml
java -jar build/libs/inboxpilot-0.1.0-SNAPSHOT.jar \
  --spring.config.additional-location=file:./config/application-local.yml \
  labels list
```

All InboxPilot keys are below `inboxpilot`:

| Group | Purpose |
|---|---|
| `oauth` | Desktop authorization, token location, Gmail scopes |
| `scanning` | Lookback, message limit, spam/trash inclusion |
| `batching` | Gmail batch size, retries, exponential backoff |
| `reports` | Output directory, JSON/CSV/HTML formats, overwrite policy |
| `checkpoints` | Resumable scan file and write interval |
| `ai` | AI opt-in, allowed redacted fields, confidence threshold |

Invalid or contradictory values stop startup and identify the failing key.
Defaults are in `src/main/resources/application.yml`; the example file explains
every setting and its safe default.

## Inventory and reports

Mailbox discovery reads message IDs in pages and metadata in batches. Bodies and
attachments are not fetched. Inventory aggregates exact senders and domains,
message/unread counts, first/last timestamps, current labels, and representative
subjects.

Configured inventory exports are written under
`inboxpilot.reports.output-directory`:

| Artifact | Purpose |
|---|---|
| `inventory.json` | Machine-readable sender/domain inventory |
| `inventory.csv` | Spreadsheet-friendly inventory |
| `inventory.html` | Browser report with summary cards and sortable tables |
| `classification-rollback.json` | Inverse label operations saved before execution |
| `accepted-ai-rules.yaml` | Human-confirmed AI suggestions converted to rules |

Existing inventory files are replaced only when `reports.overwrite` is true.
Rollback and accepted-rule artifacts are application state and are updated by
their explicit workflows.

## Analysis

The analysis layer can report:

- unclassified senders and domains;
- incompatible sender labels;
- unused, empty, and duplicate labels;
- newsletter and bulk-mail senders;
- `List-Unsubscribe` mailto and HTTPS capabilities;
- cleanup candidates and proposed label taxonomy changes.

Analysis is read-only. The HTML unsubscribe report presents links for manual
review and never opens them or sends mail.

## YAML rules

Start from [`rules.example.yaml`](rules.example.yaml). A rule file contains a
schema version and ordered rules with:

- stable lowercase ID and non-negative priority;
- leaf or composite match condition;
- one or more `add-label`/`remove-label` actions;
- `continue` or `stop` behavior;
- description, source, tags, and optional examples.

Supported leaf operators are `sender-exact`, `sender-domain`,
`sender-domain-suffix`, `subject-contains`, and `subject-regex`. Composite
operators are `all`, `any`, and `not`. Rules run by priority descending and ID
ascending. A matching `stop` rule contributes its own action and then stops
lower-priority evaluation.

Embedded `tests` assert whether sender/subject metadata should match. Invalid
YAML reports an exact path such as `rules[0].match`.

## Classification workflow

The safe workflow has four stages:

1. **Dry-run:** evaluate rules and calculate exact old, added, removed, and
   desired labels for each message. No mailbox port is available to this stage.
2. **Review:** separate clean matches, ambiguous multi-rule matches, and
   unmatched messages. Only clean matches are execution candidates.
3. **Execute:** persist `classification-rollback.json`, then send only explicit
   user-label additions/removals through Gmail batch modify.
4. **Verify:** scan fresh metadata, compare it with planned labels, and run the
   rules again. Success requires no mismatch and zero additional changes.

The execution request type rejects Gmail system labels, so classification
cannot alter read state, stars, importance, categories, inbox/archive state,
spam, trash, or deletion. Audit rows record rule ID, message ID, old labels,
planned labels, observed labels, and result.

The classification and cleanup use cases are currently application APIs; the
CLI exposes mailbox discovery and checkpoint maintenance while a richer command
surface is developed. This distinction is intentional: do not interpret a
generated plan as an executed change.

## Cleanup workflow

Cleanup detects stale newsletters, OTP/login alerts, delivery updates, and
transient notifications. Candidate reports never delete messages.

Archive preview is the default. Explicit archive execution removes only the
Gmail `INBOX` label from selected message IDs. There is no delete/trash port or
command. Unsubscribe remains manual.

## Recovery

- Resume a compatible interrupted inventory from the configured checkpoint.
- Use `checkpoint reset` only when saved progress should be discarded.
- Keep `classification-rollback.json` before executing label changes. It lists
  the inverse additions/removals for manual recovery.
- Run post-execution verification and repeat-run verification immediately.
- If OAuth scopes or credentials change, remove the cached token and authorize
  again.

## Troubleshooting

| Symptom | Action |
|---|---|
| OAuth is disabled | Set `INBOXPILOT_OAUTH_ENABLED=true` and provide both credentials |
| Browser consent repeats | Confirm the token-store path is writable and persistent |
| `invalid_grant` | Remove the cached token, verify the OAuth client, re-authorize |
| Gmail 403 during execute | Add `gmail.modify`, delete the old token, authorize again |
| Startup names a config key | Correct the malformed or contradictory value shown |
| Report already exists | Choose a new directory or deliberately enable `reports.overwrite` |
| Checkpoint fingerprint mismatch | Restore the original scan settings or explicitly reset |
| Partial Gmail batch | Review warnings; successful items remain in the result and retryable failures are retried |
| Verification mismatch | Stop, inspect audit/rollback artifacts, and do not repeat execution blindly |

Run the full local verification suite with:

```bash
./gradlew build
```
