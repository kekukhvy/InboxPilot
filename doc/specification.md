# InboxPilot design specification

## Purpose and safety model

InboxPilot inventories and analyzes a Gmail mailbox, proposes classifications,
and eventually applies explicit cleanup actions. Mailbox mutation is never an
implicit consequence of scanning or analysis: dry-run is the default, and only
an explicit execute step may change Gmail state.

Processing degrades per message where a usable partial result is possible. A
malformed message or a single provider rejection must be reported without
silently making an inventory look complete. Configuration and authorization
failures that prevent any useful work fail early with an actionable error.

## Architecture

InboxPilot uses hexagonal architecture within one Gradle module. Package
boundaries act as layers:

| Package | Responsibility | Allowed project dependencies |
|---|---|---|
| `dev.inboxpilot.domain` | Core model, invariants, and rules | none |
| `dev.inboxpilot.application` | Use cases and provider-independent ports | `domain` |
| `dev.inboxpilot.infrastructure` | Gmail, configuration, storage, and observability adapters | `application`, `domain` |
| `dev.inboxpilot.cli` | User entry points | `application`, `domain` |
| `dev.inboxpilot` | Composition root | all layers |

Dependencies point inward. Domain code contains no Spring, Google, Jackson, or
logging types. Google client types stay in infrastructure and are translated at
application-owned ports. These rules are enforced by `ArchitectureTest` and
explained in [ADR 0001](adr/0001-hexagonal-architecture.md).

## Error and observability model

Operations carry structured context such as operation name, message id,
attempt, elapsed time, outcome, and failure classification. Recoverable failures
are classified `TRANSIENT`; failures that require a configuration or data change
are `PERMANENT`. Domain remains logging-free, so adapters log failures where
they are caught, once, with the operation context attached.

Secrets, OAuth tokens, and credential payloads must never be logged.

## Gmail desktop authorization

The application owns the `CredentialProvider` port. It returns an immutable,
provider-independent `AccessToken`; the Google implementation lives in
`infrastructure.gmail.auth`. No caller outside infrastructure handles Google
credential types.

Authorization uses Google's installed-application authorization-code flow with
a local loopback receiver. The first authorization opens the system browser;
later authorizations reuse the refresh token stored by the Google client.

Before starting the browser flow, the adapter requires OAuth to be enabled and
requires non-blank client id and client secret values. Configuration, token
store, revoked-token, I/O, and transport-security failures are translated to
`CredentialProviderException` with an operator-actionable message. These are
permanent failures because retrying without changing configuration or
re-authorizing cannot resolve them.

The token store defaults to `~/.inboxpilot/tokens`, outside the source tree. Its
directory is created automatically and restricted to owner-only POSIX
permissions where supported. Filesystems without a POSIX permission view retain
their platform defaults and emit a warning; inability to create or secure the
directory is a permanent failure.

Configured scopes are requested exactly as supplied. The shipped configuration
contains only `gmail.readonly`. Broader scopes such as `gmail.modify` require an
explicit configuration change. The detailed rationale is recorded in
[ADR 0002](adr/0002-desktop-oauth-authorization.md).

## Gmail mailbox gateway

The application-owned `MailboxGateway` port exposes provider-independent label
and message-id values. Its Gmail adapter lists all labels and accepts a Gmail
search expression for message discovery. Message listing follows every
`nextPageToken` until the complete result has been collected; a repeated token
is reported as a failure instead of allowing an unbounded loop.

The generated Google client and its response types remain inside
`infrastructure.gmail`. The adapter requests an access token lazily through the
`CredentialProvider`, caches the authorized Gmail client until that token
expires, and sends no mailbox mutation request. Missing message collections are
treated as empty pages. Provider I/O failures are translated to
`MailboxGatewayException` with the original cause retained.

The message source discovers IDs with an `after:<epoch-seconds>` Gmail query,
then retrieves messages through Gmail HTTP batch requests. Each request uses the
`metadata` format and restricts the response to the message ID, internal date,
label IDs, and the `From` and `Subject` headers, so message bodies are never
downloaded. The configurable batch size defaults to and is capped at 50.

Batch callbacks capture failures against their individual message IDs. Failed
items are reported as warnings while successful metadata from the same batch is
retained and translated into provider-independent `MailMessage` values. A
failure of the enclosing HTTP batch remains a mailbox read failure because no
per-message response is then available.

Retryable item failures include Gmail quota and concurrent-request errors, HTTP
429 responses, and transient 5xx responses. Only failed message IDs are retried;
successful items are never requested twice. Retry delays use capped exponential
backoff with jitter. A throttling response halves the batch size, down to one,
for the remainder of the scan so request pressure adapts automatically.

The existing CLI exposes this slice through the read-only `labels list` and
`messages list --query=<gmail-query>` commands. Results are rendered one per
line; labels contain their stable ID and visible name, while message discovery
prints IDs only. Starting the application without a command performs no Gmail
request, preserving the unconfigured development startup path.

## Persisted contracts

OAuth refresh-token persistence is owned by Google's file data-store
implementation and is not exposed as an InboxPilot model. Future InboxPilot
checkpoints, reports, or other serialized models are versioned contracts:
incompatible field changes must update their version marker and handle older
versions deliberately.

Scan checkpoints use a versioned binary contract containing the canonical
configuration/query fingerprint and every provider-independent message gathered
so far. Writes go to a same-directory temporary file and replace the checkpoint
with an atomic move; filesystems without atomic-move support reject the write.
Resume rejects a fingerprint mismatch instead of combining incompatible scan
state. `checkpoint reset` is the explicit destructive command for discarding
saved progress.

## Inventory aggregation

Inventory aggregation is a framework-free domain operation over `MailMessage`
values. It produces deterministic, key-sorted views for exact sender addresses
and sender domains. Each entry records total and unread counts, first and last
received timestamps, the sorted union of current labels, and up to five sample
subjects selected from oldest to newest messages. The Gmail `UNREAD` label is
the source of unread counts.

Inventory reports are deterministic contracts. CSV emits one stable header and
sender/domain rows with RFC-style quoting for commas, quotes, and line breaks.
JSON uses a stable field order and properly escapes string values. Both formats
preserve the sorted inventory order, making repeated exports byte-for-byte
comparable when the source inventory is unchanged.

Inventory progress is published after each completed metadata batch and before
each retry. Snapshots contain processed and remaining message counts, cumulative
retry count, elapsed time, throughput in messages per second, and ETA when a
non-zero throughput makes it calculable. The application publishes through a
port; the infrastructure adapter renders the snapshot to structured logs.

The Gmail adapter is verified at the HTTP boundary with deterministic mocked
responses. Integration fixtures cover page-token traversal, multipart batch
success and per-item throttling failures, retrying only failed message IDs, and
filesystem checkpoint continuation. They require neither network access nor a
real mailbox.

## Mailbox analysis

Unclassified inventory analysis accepts the set of user-created label IDs,
excludes sender and domain entries carrying any of them, and ranks the remaining
entries by descending message volume, then unread count, then stable key. Gmail
system labels do not make an entry user-classified.

Sender-label conflict analysis accepts explicit incompatible-label groups.
Whenever a sender carries at least two labels from one group, the report records
the sorted conflicting labels and that sender's representative sample subjects.

Label-structure analysis compares all known labels with labels observed during
the inventory scan. Labels absent from the scan are reported as unused or empty;
visible names equal after trimming and case folding are grouped as duplicate
consolidation candidates. Results are deterministic and never mutate labels.

Newsletter and bulk-mail detection uses metadata only. A sender is reported
when messages expose a non-empty `List-Unsubscribe` header, bulk/list/junk
`Precedence`, or a conventional newsletter/notification sender name. Each
result includes the matched evidence so recommendations remain explainable.

`List-Unsubscribe` capability extraction accepts only angle-bracketed `mailto`
and HTTPS URIs. Malformed, insecure, and unsupported mechanisms are ignored.
Parsing is inert: extraction never opens a URL, sends mail, or changes mailbox
state.

Cleanup candidate analysis is metadata-only and requires an explicit age
cutoff. It classifies old newsletters, obsolete alerts, OTP messages, and
promotional traffic into an immutable review report. The report has no mutation
or deletion operation; mailbox changes remain a separate explicit execute step.

Label taxonomy planning emits evidence-backed `CREATE`, `MERGE`, and `RENAME`
proposals. New labels require an explicit mailbox-volume threshold, duplicate
labels become merge candidates, and non-canonical visible names become rename
candidates. Planning is read-only and produces no provider operations.

HTML analysis export is a self-contained UTF-8 document with summary cards,
anchor navigation, and client-side sortable sender and domain tables. All
mailbox-derived values are HTML-escaped before rendering, and the document does
not load remote resources.

Rule files are versioned YAML documents. Schema version 1 defines a stable rule
ID, non-negative priority, one recursive match-condition tree, one or more
provider-independent actions, explicit `continue`/`stop` conflict behavior, and
description/source/tag metadata. IDs use lowercase letters, digits, hyphens,
and underscores. Persisted collections are ordered and immutable after loading;
`doc/rules.example.yaml` is the canonical example.

Rule YAML is loaded through SnakeYAML's safe constructor with duplicate keys
disabled and bounded document size and aliases. Validation failures name the
exact field path (for example, `rules[0].match`) and malformed YAML is
translated into the same actionable error type. No partially valid rule set is
returned.

Leaf rule conditions support exact sender, exact sender domain, boundary-aware
domain suffix, subject substring, and subject regular expression matching. Each
condition explicitly controls case sensitivity with `ignore-case`; regular
expressions are compiled once and invalid expressions fail validation before
evaluation.

Composite rule conditions use recursive `all`, `any`, and `not` nodes. `all`
and `any` require at least one ordered operand; `not` requires exactly one.
Evaluation is deterministic and short-circuits operands from left to right.

Rules evaluate by descending numeric priority, with ascending rule ID as the
stable tie-breaker. Every matching rule contributes its action specifications;
a matching `stop` rule contributes its own actions and then prevents all lower
ordered rules from being considered. Non-matching stop rules have no effect.

The initial rule action set contains only `add-label` and `remove-label` with a
required non-blank label name. Evaluation compiles matched actions into an
ordered, rule-attributed `LabelChangePlan`; it never applies that plan. Archive,
delete, and unknown actions fail validation and are excluded from this release.

Rules may embed ordered metadata-only examples under `tests`. Every example has
a name, sender, subject, and boolean `expect-match`; the rule test runner reports
the expected and actual result without starting Spring or contacting a mailbox.
The field is optional, so existing schema-version-1 documents remain compatible.

## Current limitations and deferred work

- The OAuth browser round-trip requires a real browser, loopback port, Google
  Cloud desktop client, and user consent; it is not exercised by hermetic unit
  tests.
- No real-mailbox verification target is configured, so live authorization is
  reported as not run because of the environment, never as passing.
- Live gateway verification requires a configured Gmail account and is not run
  because no verification mailbox is configured.
- Splitting package layers into separate Gradle modules remains deliberately
  deferred while ArchUnit provides the required boundary enforcement.
