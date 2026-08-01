# SDLC run record — issue #7 (Baseline unit and architecture tests)

Branch `7-baseline-tests`, slice `main..HEAD`.

- tdd-implementer | 2026-08-01 | skipped: this slice adds tests to existing production code, which is test-author's job, not test-first implementation; no production code changed
- spec-keeper | 2026-08-01 | skipped: no design spec exists yet (doc/specification.md absent); no model, format, or architecture changed — the conventions encode decisions ADR 0001 already records
- user-docs-writer | 2026-08-01 | skipped: no user-visible surface changed — no flag, config key, endpoint, or output artifact; the slice is test-only
- test-author | 2026-08-01 | ran: MessageIdTest (7 cases incl. 4 parameterised blanks), GmailMessageSourceTest (3), InboxPilotCommandTest (3), plus 4 convention rules in ArchitectureTest | files: 4
- javadoc-writer | 2026-08-01 | skipped: no public or protected production API added; the new types are package-private test classes, documented by class-level Javadoc where the intent was not obvious
- logging-instrumenter | 2026-08-01 | skipped: no production code changed, so there is nothing new to instrument; one new rule *enforces* logging by failing the build on System.out/err

## Scope note

Issue #7 is worded broadly ("baseline unit and architecture tests"), but JUnit 5
was configured in #2, `ArchitectureTest` written in #3, and config/error/
observability covered in #4-#5. Rather than restate that coverage, this slice
closes the actual gaps: `MessageId`, `GmailMessageSource`, `InboxPilotCommand`,
and conventions the layered rules did not check.

## Verification

Every new architecture rule was mutation-checked — a violation was introduced,
the matching rule confirmed to fail, then the violation reverted:

| Rule | Violation used | Result |
|---|---|---|
| domain has no Spring annotations | `@Component` on `MessageId` | FAILED as expected |
| nothing writes to the console | `System.out.println` in `InboxPilotCommand` | FAILED as expected |
| ports are interfaces | a `class` in `application.port` | FAILED as expected |

The fourth rule (no production dependency on JUnit/AssertJ/Mockito) was not
mutated: violating it requires changing the Gradle configuration, and it uses the
same `noClasses().dependOnClassesThat()` form as the three rules above.
