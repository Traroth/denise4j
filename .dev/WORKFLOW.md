# Workflow — denise4j

This document defines the standard workflow for implementing or modifying
a class in the library. The goal is to maintain architectural consistency,
code quality, and reliable behaviour when using AI-assisted development.

All steps should be followed in order unless explicitly instructed otherwise.

---

## Overview

The development workflow follows a **design‑first, TDD‑driven approach**:

1. Discussion
2. Stub class + JML specifications
3. Design review
4. Update `ARCHITECTURE.md`
5. Update `BACKLOG.md`
6. Tests (black‑box, TDD)
7. Implementation
8. White‑box tests
9. Static analysis
10. JML conformance check (JML → Code)
11. JML completeness check (Code → JML)
12. Test coverage review
13. Documentation
14. Session wrap‑up

Each step has a specific purpose and should not be skipped.

---

## Model selection

| Step | Model | Reason |
|------|-------|--------|
| 1 — Discussion | **Opus** | Open-ended architectural reasoning; errors here propagate to all subsequent steps |
| 2 — Stub + JML | **Sonnet** | Precision required; structure already defined by step 1 |
| 3 — Design review | **Sonnet** | Validation against an existing design |
| 4 — Update ARCHITECTURE.md / INVARIANTS.md | **Sonnet** | Reformulating complex decisions into durable documentation |
| 5 — Update BACKLOG.md | **Haiku** | Mechanical, well-structured format |
| 6 — Tests (TDD) | **Sonnet** | Contractual precision required |
| 7 — Implementation | **Sonnet** | Code generation with fidelity to upstream decisions |
| 8 — White-box tests | **Sonnet** | Knowledge of internals required |
| 9 — Static analysis | **Sonnet** | Structured analysis against known criteria |
| 10 — JML conformance check | **Sonnet** | Requires reading code against formal specs |
| 11 — JML completeness check | **Sonnet** | Requires identifying missing formal specs |
| 12 — Test coverage review | **Sonnet** | Structured analysis against known criteria |
| 13 — Documentation | **Sonnet** / **Haiku** | Sonnet if the API changed; Haiku for minor updates |
| 14 — Session wrap-up | **Haiku** | Mechanical backlog updates |

---

## 1. Discussion

**Use Plan mode** — no code modifications should be made during this step.

Before writing any code, discuss the design of the effect or class.

Define:

- The **input and output** (what image(s) go in, what comes out)
- The **core algorithm** (pixel mapping, palette operation, compositing mode)
- The **class invariants** (image dimensions, pixel range, parameter constraints)
- The **expected performance** characteristics (per-pixel cost, memory usage)
- Whether the effect operates **in-place** or produces a new image
- Whether **parameters** are fixed at construction or passed per call

Do not move to the next step until the design is clearly understood and agreed upon.

---

## 2. Stub class + JML specifications

Create a skeleton of the class where:

- All public methods are declared with their full signature and Javadoc
- Method bodies contain only `throw new UnsupportedOperationException()`
- Class invariants are expressed as JML `@invariant` annotations
- Pre‑ and post‑conditions of key methods are expressed as JML
  `@requires` / `@ensures` contracts

The goal of this step is to make the **contract explicit and reviewable**
before any logic is written.

---

## 3. Design review

**Use Plan mode** — no code modifications should be made during this step.

Run the `design-review` skill.

The goal is to validate:

- the correctness of the algorithm
- the clarity of invariants
- the pixel-level correctness guarantees
- performance implications
- possible alternative implementations

---

## 4. Update ARCHITECTURE.md and INVARIANTS.md

### ARCHITECTURE.md

Add a section describing:

- the chosen algorithm
- the invariants
- key implementation choices
- alternatives considered and rejected

### INVARIANTS.md

Add a section for the new class documenting:

- null handling policy
- image dimension constraints
- pixel value range invariants
- parameter validity invariants

Express invariants using JML `@invariant` notation where possible.

---

## 5. Update BACKLOG.md

Add the class and its tasks to the backlog. Move completed tasks to
the `## Done` section immediately when they are finished.

---

## 6. Tests — black‑box (TDD)

Write the black‑box tests **before** implementing the class, against the stub.

#### Black‑box tests (`ClassNameBlackBoxTest`)

- Verify the public contract only
- Cover: nominal behaviour, boundary conditions, error scenarios
- Must be writable without reading the implementation

All tests are expected to fail at this point — the stub throws
`UnsupportedOperationException`. This is intentional.

---

## 7. Implementation

Replace each `UnsupportedOperationException` stub with a real implementation,
following `.dev/standards/JAVA_STANDARDS.md`.

The goal is to make all black‑box tests pass. Once they do, run the **full
test suite** (`mvn test`) to verify no existing class was broken.

Key principles:

- Preserve class invariants
- Prefer clarity over cleverness
- Prefer direct pixel array access (`getRaster().getDataBuffer()` or
  `getRGB`/`setRGB`) over Graphics2D when the effect requires per-pixel control
- Avoid allocating inside hot loops

---

## 8. White‑box tests

Write the white‑box tests **after** the implementation is complete.

`ClassNameWhiteBoxTest`

These tests target implementation risks, such as:

- boundary pixels (first row, last row, corners)
- wrap-around behaviour in scrolling/rotation
- integer overflow in coordinate transformations
- rounding behaviour in zoom/rotation

Each test must have a comment explaining **why the scenario is risky**.

---

## 9. Static analysis

Run the `static-analysis` skill.

Resolve all **Critical issues** before continuing.

---

## 10. JML conformance check (JML → Code)

Run the `jml-conformance-check` skill.

If a violation is found, return to step 7, fix the code, and re‑run
the full test suite before resuming.

---

## 11. JML completeness check (Code → JML)

Run the `jml-completeness-check` skill.

If gaps are found, add the missing annotations, then re‑run step 10.

---

## 12. Test coverage review

Run the `test-coverage-review` skill.

---

## 13. Documentation

Update `ARCHITECTURE.md` and `INVARIANTS.md` when public behaviour changes.

---

## 14. Session wrap‑up

At the end of a session, run `update-backlog`.

This step should never be skipped.

---

## Guiding principles

### Design before code

Never implement an effect before its invariants and behaviour are defined.

### Fix known issues before adding new code

Never advance on new code while known bugs or issues remain unresolved.

### Invariants must always hold

All methods must preserve class invariants. If invariants are temporarily
broken during a method, they must be restored before returning.

### Prefer clarity over cleverness

Readable and maintainable code is preferred over overly clever implementations.

### Small iterations

Prefer incremental development:

1. scaffold the structure
2. implement minimal behaviour
3. validate invariants
4. extend functionality