---
name: jml-design
description: >
  Write JML specifications (invariants, requires, ensures) for a class in
  denise4j. Use this skill during step 2 of the workflow (stub + JML)
  or when adding JML annotations to an existing class. The goal is to produce
  a complete, consistent, and verifiable formal contract before implementation.
---

# JML Design — denise4j

## When to run

- During step 2 of the workflow (stub class + JML specifications)
- When adding JML annotations to an existing class
- When the user says "add JML", "write contracts", or "specify invariants"

---

## Inputs

Before starting, read:

1. The class stub or source file
2. The relevant section of `INVARIANTS.md` (if it exists)
3. The class Javadoc (the informal contract)

---

## Specification checklist

### 1. Class-level invariants (`/*@ ... @*/`)

Place the invariant block inside the class body, before the first field.

- [ ] **Parameter validity** — all configurable parameters are within their valid domain
- [ ] **Null handling** — non-null constraints on required fields

### 2. Constructor contracts (`//@ ensures`)

- [ ] Post-conditions on configuration fields
- [ ] Pre-conditions for parameters with constraints (e.g. `scrollAmount >= 0`)

### 3. Query method contracts

- [ ] Return value constraints (`\result != null` for image-returning methods)

### 4. Mutation method contracts

For each effect-applying method:

- [ ] **Pre-conditions** (`//@ requires`) — null rejection, dimension constraints
- [ ] **Post-conditions** (`//@ ensures`) — output image non-null, dimensions preserved
- [ ] **Frame conditions** — what is *not* changed (e.g. input image unmodified for non-in-place effects)

---

## JML syntax reference

```java
// Class-level invariant block (inside class body, before first field)
    /*@
      @ public invariant scrollAmount >= 0;
      @ public invariant scrollAmount < width;
      @*/

// Method-level annotations (after Javadoc, before @Override)
//@ requires image != null;
//@ ensures \result != null;
//@ ensures \result.getWidth() == image.getWidth();
//@ ensures \result.getHeight() == image.getHeight();
```

---

## Consistency rules

- All invariants in `INVARIANTS.md` must have a corresponding JML annotation in the code
- JML annotations must not reference private fields — use public methods only
- Pre-conditions must match the exceptions documented in Javadoc

---

## Output format

Report the annotations added, grouped by category:

1. **Invariants** — list each class-level invariant
2. **Constructors** — list contracts per constructor
3. **Methods** — list contracts per method

Flag any inconsistency found between `INVARIANTS.md` and the code.