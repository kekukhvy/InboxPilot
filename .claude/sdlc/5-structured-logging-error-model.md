# SDLC run record — issue #5 (Structured logging and error model)

Branch `5-structured-logging-error-model`, slice `main..HEAD`.

- tdd-implementer | 2026-08-01 | skipped: implemented inline; each behaviour was mutation-checked instead — the nesting fix and the inherit-clearing were reverted one at a time and the matching test confirmed to fail before being trusted
- spec-keeper | 2026-08-01 | skipped: no design spec exists yet (doc/specification.md absent); the error taxonomy and MDC field contract are documented in README + Javadoc
- user-docs-writer | 2026-08-01 | ran: README "Reading the logs" — worked example, field table, transient/permanent meaning, grep recipe, and the logging.pattern.console key | files: 1
- test-author | 2026-08-01 | ran: InboxPilotExceptionTest (9 cases: classification, cause chaining, null rejection, enum exhaustiveness) + OperationContextTest (16 cases: full context, nesting/restore/no-inherit, outcomes, thread confinement, idempotent close) | files: 2
- javadoc-writer | 2026-08-01 | ran: Javadoc on all 5 production types explaining why each choice exists (why retryability is the branch point, why the MDC over per-message fields, why not thread-safe) | files: 5
- logging-instrumenter | 2026-08-01 | ran: OperationContext warns when an operation closes without an outcome, emitted while the context is still attached; domain deliberately left silent per ADR 0001, enforced by ArchitectureTest | files: 1

## Notes

- `domain` stays framework-free: the error model imports only `java.util`.
  `ArchitectureTest.DomainLayer.doesNotLog` fails the build if SLF4J reaches it.
- MDC key order is not deterministic — Logback backs the MDC with a `HashMap`.
  Verified empirically, not assumed; recorded in `application.yml` and the README
  so nobody relies on field position.
