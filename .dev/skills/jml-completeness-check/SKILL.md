---
name: jml-completeness-check
description: >
  Verify that the JML specifications are complete and up to date with
  respect to the implementation. Direction: Code -> JML. Use this skill
  after implementation is stable (after step 8) or after refactoring.
---

# JML Completeness Check — denise4j

## Purpose

Verify that the **JML specifications are complete and up to date** with
respect to the code. Given the implementation, are all behaviours formally
specified?

---

## When to run

- After step 8 (white-box tests) — the implementation is stable
- After refactoring a class that has JML annotations
- After adding a public method to a class with existing JML contracts

---

## Inputs

Before starting, read:

1. The class source file (implementation + JML annotations)
2. The relevant section of `INVARIANTS.md`
3. The class Javadoc (the informal contract)

---

## Checklist

### 1. Invariant completeness

- [ ] **Parameter validity** — are all parameter constraints expressed as invariants?
- [ ] **Null handling** — is there an invariant on required non-null fields?
- [ ] **Implicit invariants** — are there conditions that hold in the code but
  are not formalized?

### 2. Constructor completeness

- [ ] **Pre-conditions present** — parameters with constraints have `@requires`
- [ ] **Post-conditions present** — configuration state after construction is specified

### 3. Method completeness

For each public method:

- [ ] **Has JML annotations** — every non-trivial public method has `@requires` or `@ensures`
- [ ] **Return value specified** — methods returning an image have `@ensures \result != null`
- [ ] **Dimension preservation specified** — if the output has the same size as the input,
  this is expressed with `@ensures`
- [ ] **Overriding methods** — specs start with `//@ also`

### 4. Cross-check with INVARIANTS.md

- [ ] Every invariant in `INVARIANTS.md` has a corresponding `@invariant` in the code
- [ ] Every `@invariant` in the code is documented in `INVARIANTS.md`

---

## Output format

1. **Missing invariants**
2. **Missing method contracts**
3. **Incomplete contracts**
4. **Stale contracts**
5. **INVARIANTS.md drift**