# Changelog

All notable InboxPilot changes are recorded in GitHub releases. Release notes
are generated from merged pull requests and grouped by their `type:*` labels
using `.github/release.yml`.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and release tags follow semantic versioning.

## Unreleased

### Added

- Local-first Gmail OAuth, resilient metadata inventory, and resumable scans.
- JSON, CSV, and interactive HTML reports.
- Sender, label, bulk-mail, unsubscribe, cleanup, and taxonomy analysis.
- Declarative YAML rules with embedded tests and deterministic evaluation.
- Dry-run classification, explicit label execution, audit, rollback, and
  verification workflows.
- Safe archive previews, deletion-candidate reports, and manual unsubscribe
  review.
- Disabled-by-default, redacted AI classification boundary.
- Runnable JAR, ZIP, TAR.GZ, and OCI buildpack packaging.

## Release process

1. Merge issue PRs with the correct `type:*` label and update the Unreleased
   section when a change needs durable migration or compatibility guidance.
2. Create and push a semantic tag such as `v0.1.0` from a green `main` commit.
3. The Release workflow runs all tests, builds versioned artifacts and
   checksums, generates categorized notes, and publishes the GitHub release.

Do not create release assets manually: the tag is the immutable source for the
version embedded in every archive.
