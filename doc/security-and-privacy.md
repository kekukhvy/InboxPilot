# Security and privacy

InboxPilot is a local-first Gmail organizer. It requests only configured Gmail
scopes, stores OAuth state and generated artifacts locally, and keeps mailbox
changes behind explicit execution APIs. This document describes the trust
boundaries and the operator responsibilities that remain.

## Security model

InboxPilot assumes the computer and operating-system account running it are
trusted. Protect that account, the OAuth token store, configuration overrides,
reports, checkpoints, rollback files, and logs as sensitive mailbox data. Do
not commit them or place them in a shared directory.

The main risks are stolen OAuth credentials or refresh tokens, disclosure of
message metadata in local artifacts, overly broad Gmail permissions, unsafe
rules changing labels at scale, and disclosure to an optional AI provider.
InboxPilot reduces those risks with least-privilege defaults, metadata-only
retrieval, dry-run planning, restricted mutation ports, rollback artifacts,
and disabled-by-default AI integration.

## Gmail access and OAuth scopes

The default scope is:

```text
https://www.googleapis.com/auth/gmail.readonly
```

It permits inventory, analysis, reports, and classification previews, but not
mailbox changes. Explicit label or archive execution requires adding:

```text
https://www.googleapis.com/auth/gmail.modify
```

Do not request broader scopes such as full mailbox access. InboxPilot has no
delete/trash adapter. Classification accepts only user-label additions and
removals; archive execution removes only the `INBOX` label.

Changing scopes does not upgrade an already cached grant. Delete the cached
token and authorize again after changing the configured scope list. Review and
revoke grants at your Google Account's third-party access page when a machine
is lost, a token may have leaked, or InboxPilot is no longer used.

## Credentials and token storage

Pass the OAuth client ID and client secret through environment variables or an
untracked local configuration file. Never put credentials in source control,
command examples committed to the repository, reports, or logs.

The refresh token is cached at `${user.home}/.inboxpilot/tokens` by default.
Use a directory readable only by the operating-system account that runs
InboxPilot, keep it outside the repository and synchronized folders, and avoid
backing it up unless the backup is encrypted and access-controlled. A refresh
token grants continuing access within its approved scopes until revoked.

If credentials or tokens may be compromised:

1. Revoke the InboxPilot grant in the Google Account.
2. Remove the local token store.
3. Rotate the OAuth client secret if it was exposed.
4. Re-authorize with the minimum required scopes.
5. Inspect Google Account activity and InboxPilot audit artifacts.

## Data retrieved from Gmail

Inventory fetches message IDs and metadata: sender, subject, timestamp, label
IDs, and selected headers such as `List-Unsubscribe`. It does not request body
content or attachments. Subjects and sender addresses can still contain
personal or confidential information and must be protected accordingly.

Gmail access tokens are held only long enough to call the API. Application
logging records operational context and failures, not OAuth secrets or bearer
tokens. Keep production log levels at their defaults; inspect verbose logs
before sharing them because message IDs and operational metadata may appear.

## Local artifacts and retention

Depending on the workflow, InboxPilot can create:

- inventory JSON, CSV, and HTML containing senders, domains, subjects, counts,
  labels, and timestamps;
- a checkpoint containing scan progress and message identifiers;
- classification audit and rollback data containing message and label IDs;
- accepted AI rules containing human-approved sender/domain conditions;
- user-authored YAML rules, which may identify people or organizations.

Files are written only to configured local paths. Existing inventory reports
are not overwritten unless `inboxpilot.reports.overwrite` is enabled. InboxPilot
does not upload reports or automatically delete them.

Choose retention periods appropriate for the mailbox and delete obsolete
artifacts using normal operating-system controls. Before sharing a report,
remove subjects, sender addresses, message IDs, and label names that the
recipient does not need. Encrypt disks and backups that contain these files.

## Optional AI boundary

AI classification is disabled by default. Enabling it is an explicit opt-in:

```yaml
inboxpilot:
  ai:
    enabled: true
    include-redacted-subject: false
    include-label-ids: false
    minimum-automatic-confidence: 0.90
```

The provider port receives no body or attachment. The sender is reduced to its
domain. Subjects are omitted by default; when enabled, local redaction replaces
email addresses, URLs, and long numbers before provider invocation. Label IDs
are also omitted by default. Redaction lowers exposure but is not a guarantee
that a subject is anonymous: names, account references, or unusual phrases may
remain.

Provider retention, training, residency, subprocessors, and access controls are
outside InboxPilot's boundary. Evaluate those terms before supplying any data.
Keep subject and label sharing disabled unless required. Suggestions below the
configured confidence threshold require human review, and only explicitly
accepted suggestions are persisted as deterministic YAML rules.

## Safe operation checklist

Before an execute step:

1. Run inventory and classification in read-only/dry-run mode.
2. Review ambiguous, unmatched, cleanup, and unsubscribe reports manually.
3. Inspect exact message IDs and label additions/removals.
4. Confirm the OAuth grant has no scope broader than required.
5. Preserve the generated rollback artifact.
6. Execute only the reviewed batch, then run verification and a repeat dry-run.

Never treat a cleanup candidate or unsubscribe link as authorization to delete,
archive, open a URL, or send mail. Those reports are recommendations only.

## Reporting a vulnerability

Do not open a public issue containing credentials, tokens, mailbox metadata, or
an exploitable proof of concept. Use GitHub's private vulnerability reporting
for the repository when available. Revoke any exposed grant immediately; a
documentation report does not invalidate leaked credentials.
