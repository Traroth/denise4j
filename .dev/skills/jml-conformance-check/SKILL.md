---
name: jml-conformance-check
description: >
  Verify that the implementation conforms to its JML specifications
  (invariants, requires, ensures). Direction: JML -> Code. Use this skill
  after implementation (step 7) or after modifying a class that has JML
  annotations.
---

# JML Conformance Check — denise4j

## Purpose

Verify that the **code conforms to the JML specifications**. Given the
contracts, does the implementation respect them?

---

## When to run

- After step 7 (implementation) when JML specs were written at step 2
- After modifying a method that has JML contracts
- After fixing a bug — to verify the fix didn't break a contract

---

## Inputs

Before starting, read:

1. The class source file (implementation + JML annotations)
2. The relevant section of `INVARIANTS.md`

---

## Checklist

### 1. Class invariants

For each `@invariant` annotation:

- [ ] **Established by constructors** — every constructor leaves the object
  in a state satisfying all invariants
- [ ] **Preserved by all methods** — every method restores invariants before returning
- [ ] **Temporarily broken invariants** — if broken mid-method, verified restored
  before any return path

### 2. Pre-conditions (`@requires`)

- [ ] **Validated or assumed** — the method checks the pre-condition and throws
  an appropriate exception, or documents that the caller is responsible
- [ ] **Consistent with Javadoc** — the `@requires` clause matches the `@throws`
  documentation

### 3. Post-conditions (`@ensures`)

- [ ] **Satisfied on all return paths** — including edge cases
- [ ] **Return value correct** — if the contract specifies `\result`, verify it
- [ ] **Output dimensions correct** — if the contract specifies dimension preservation,
  verify the output image has the expected size

### 4. Purity annotations

- [ ] Methods annotated `/*@ pure @*/` have no side effects

### 5. `also` keyword

- [ ] Every JML spec on a method that overrides a parent starts with `//@ also`

---

## Output format

Report findings grouped by severity:

1. **Violations** — the code does not satisfy a JML contract
2. **Risks** — the code probably satisfies the contract but the reasoning is fragile
3. **OK** — contracts verified, no issues found