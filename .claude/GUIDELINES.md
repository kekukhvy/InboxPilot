# Coding Guidelines

These rules are **mandatory** for every change. They apply to humans and to
Claude equally. If a rule conflicts with "just make it work", the rule wins —
ask before breaking one.

---

## Foundations

We follow **Clean Code** and **Clean Architecture**.

- **Clean Architecture** — dependencies point one way. The innermost module
  depends on nothing else in the project; outer modules depend inward. The
  concrete module list and dependency rule are in [PROJECT.md](./PROJECT.md).
- **Clean Code** — code reads like prose. Intention-revealing names, small
  functions, no surprises.

---

## Hard rules (non-negotiable)

### 1. No literals in code — ever
Magic literals are **strictly forbidden**. Every value must live in a
**constant** or a **variable**, never inline.

- String literals → `private static final String` constants (or config).
- Numbers → named constants. The only allowed bare numbers are `0` and `1`
  in genuinely trivial idioms (loop start, `size - 1`); even then prefer a
  name if it carries meaning.
- Repeated/meaningful values → a single named constant, never duplicated.
- Configuration values (paths, timeouts, limits) → config or CLI options, not
  hardcoded.

```java
// ❌ forbidden
if (path.endsWith(".java") && lines > 40) { warn("too long"); }

// ✅ required
private static final String JAVA_EXTENSION = ".java";
private static final int MAX_METHOD_LINES = 40;
if (path.endsWith(JAVA_EXTENSION) && lines > MAX_METHOD_LINES) { ... }
```

Prefer enums over string constants where the value is a closed set (a status, a
kind, a category, a layer).

### 2. Methods ≤ 40 lines
No method exceeds **40 lines** (body, excluding signature and braces). If it
grows past that, extract sub-methods with intention-revealing names. A long
method is a sign of a missing abstraction.

### 3. SRP — Single Responsibility Principle
Every class and method does **one** thing. One reason to change. A parser
parses; a detector detects; a renderer renders. Pipeline stages must not reach
into each other's concerns — if a parser needs git, the boundary leaked.

### 4. KISS — Keep It Simple
Choose the simplest design that solves the actual problem. No speculative
generality, no abstraction without a second caller. No deep generics. This tool
exists to make code understandable — its own code must be the example.

### 5. DRY — Don't Repeat Yourself
No copy-pasted logic, no duplicated literals, no parallel implementations of the
same rule. Shared model types live in the innermost module.

---

## Naming

- Classes: nouns (`MessageClassifier`, `RuleParser`, `SenderId`).
- Methods: verbs (`parse`, `resolve`, `detect`, `render`).
- Booleans: `is/has/supports` (`isResolved`, `hasCallers`).
- Constants: `UPPER_SNAKE_CASE`.
- No abbreviations or single-letter names (except trivial loop indices).
- Names reveal intent — no comments needed to explain what a name means.

---

## Methods & classes

- Small methods, small classes. Keep classes cohesive; split when a class starts
  to have multiple reasons to change.
- Minimize parameters. 3+ related parameters → a parameter object. Use the
  **Builder pattern** for complex model objects.
- No boolean flag parameters that switch behaviour — split into two methods.
- Fail fast: validate inputs at the start with guard clauses.
- Prefer immutability. Identifier and value types are immutable; prefer records
  for value types.

---

## Model layer specifics (innermost module)

- **No dependency on outer modules.** Enforce it with a test.
- Any persisted or serialised model is a **serialisation contract** — it is read
  back by later runs and by other tools. Changing a field is a breaking change:
  bump the version marker and handle the mismatch deliberately.
- Keep model types free of presentation concerns. No colours, no CSS classes, no
  view-specific shapes in the core — those belong to the presentation module.

---

## Error handling

- No swallowed exceptions. No empty `catch` blocks.
- **Degrade, don't fail** — where `PROJECT.md` lists this as an invariant. A
  malformed input, an unavailable dependency, a corrupt cache — each produces a
  warning and a usable result, never a crashed run. A tool that dies on one odd
  input is useless on real data.
- Report what was degraded. A silent partial result is worse than a loud one —
  the user must know the result is incomplete.
- Reserve exceptions for genuinely unrecoverable states (unreadable input root,
  invalid arguments).

---

## Comments

- Code should be self-explanatory; comments explain **why**, not **what**.
- No commented-out code. No noise comments restating the obvious.
- Keep doc-comments where they add real value (public API, non-obvious
  invariants, the reason a heuristic exists).

---

## Testing

- **Unit tests** for business, parsing, detection, and transformation logic — run
  against small fixtures checked into test resources, not against a whole real
  data set.
- **Fixtures over mocks** for anything that consumes structured input: a tiny
  real input file exercises the production parser honestly; a mocked
  intermediate representation tests nothing.
- **End-to-end runs against the verification project** named in `PROJECT.md` for
  anything non-trivial. Guard these so the suite still passes when that path is
  absent.
- Cover the degradation paths explicitly — malformed input, unresolved
  reference, missing binary, corrupt cache. They are the rules most likely to
  regress.
- Tests follow the same rules: no magic literals, clear names, one assertion
  focus per test where reasonable.

---

## Quick checklist before finishing any change

- [ ] No inline literals — all in constants/enums/config
- [ ] No method longer than 40 lines
- [ ] Each class/method has a single responsibility
- [ ] No duplicated logic or values (DRY)
- [ ] Simplest solution that works (KISS)
- [ ] Module boundaries respected; the innermost module imports nothing outward
- [ ] Failure paths degrade with a warning rather than crashing
- [ ] Tests added/updated and passing
