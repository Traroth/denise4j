---
name: jml-test-generation
description: >
  Generate test cases derived from JML specifications (invariants, requires,
  ensures) for a class in denise4j. Use this skill after JML contracts are in
  place and tests need to be written or reviewed.
---

# JML Test Generation — denise4j

## When to run

- During step 6 of the workflow (tests — TDD) after JML contracts are written
- During step 12 (test coverage review) to identify gaps
- When the user says "generate tests from JML", "test the contracts"

---

## Inputs

Before starting, read:

1. The class source file (with JML annotations)
2. The existing test files (`BlackBoxTest`, `WhiteBoxTest`)
3. `INVARIANTS.md` for the relevant class section

---

## Test derivation rules

### 1. From `@invariant` — invariant preservation tests

For each class-level invariant, generate tests that verify it holds after
every kind of mutating call.

These tests belong in **BlackBoxTest**.

### 2. From `@requires` — pre-condition tests

For each `@requires` clause, generate:

- **Nominal case** — call with inputs satisfying the pre-condition
- **Violation case** — call with inputs violating the pre-condition, expect exception

**Example:**

```java
// @requires image != null
@Test
void applyWithNullImageThrows() {
    assertThrows(NullPointerException.class, () -> effect.apply(null));
}
```

### 3. From `@ensures` — post-condition tests

For each `@ensures` clause, generate a test that verifies the post-condition.

**Example:**

```java
// @ensures \result.getWidth() == image.getWidth()
@Test
void applyPreservesWidth() {
    BufferedImage src = new BufferedImage(100, 80, BufferedImage.TYPE_INT_RGB);
    BufferedImage result = effect.apply(src);
    assertEquals(100, result.getWidth());
}
```

### 4. Boundary cases

- Zero-size images (if allowed or expected to throw)
- Single-pixel images
- Images with extreme parameter values (scroll = width - 1, zoom = 1, rotation = 0°/360°)

---

## Test placement

| Derivation source | Test level |
|---|---|
| `@invariant` (observable) | BlackBoxTest |
| `@requires` / `@ensures` (public API) | BlackBoxTest |
| Internal pixel-level edge cases | WhiteBoxTest |

---

## Output format

1. **Coverage summary** — how many JML clauses exist, how many are covered
2. **Missing tests** — list of JML clauses without corresponding tests,
   with suggested test method signatures
3. **Generated tests** — if asked to write tests, provide the complete test
   methods with comments linking back to the JML clause