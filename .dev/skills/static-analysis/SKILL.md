---
name: static-analysis
description: >
  Perform a structured static analysis of any Java class in denise4j.
  Use this skill after every class generation or significant modification —
  even if the user doesn't explicitly ask for it. Also trigger when the user
  says "analyse", "check", "review", "audit", or "is this correct".
---

# Static Analysis — denise4j

## When to run

- After generating or significantly modifying any Java class
- When the user asks for a review, audit, or correctness check
- Before marking a backlog task as done

---

# Analysis checklist

Run every item below. Report only findings — skip items with no issues.

---

# 1. Critical — bugs, contract violations, broken invariants

### Logic and contract violations

- [ ] Logic bugs (off-by-one, wrong index, unreachable branch)
- [ ] Coordinate computation errors (overflow in width * height, wrong modulo for wrap-around)
- [ ] Pixel component clamping missing (values outside 0–255 range)
- [ ] Output image dimensions wrong (width/height swapped, wrong type)
- [ ] In-place vs. out-of-place confusion (writing to source image when a copy is expected)

### Structural invariants

- [ ] Class invariants not documented (preferably using JML `@invariant`)
- [ ] Constructor does not establish all invariants
- [ ] Mutation methods break invariants

---

# 2. Major — performance, pixel correctness, null safety

### Complexity and performance

- [ ] Per-pixel allocation inside the hot loop
- [ ] Unnecessary `getRGB`/`setRGB` calls where bulk array access is more efficient
- [ ] Redundant image copies

### Pixel correctness

- [ ] Wrap-around not handled at image boundaries (scrolling, rotation)
- [ ] Bilinear or nearest-neighbour interpolation applied incorrectly
- [ ] Alpha channel handled inconsistently (ignored or corrupted)
- [ ] Integer division where floating-point is required (or vice versa)

### Null safety

- [ ] Missing null check where the contract requires it
- [ ] `null` returned where `Optional` is required (see null handling rules)

---

# 3. Minor — style, documentation, guideline compliance

### File structure

- [ ] File header missing or malformed
- [ ] Version in file header does not match `pom.xml`

### Imports

- [ ] Wildcard import present
- [ ] Imports not sorted alphabetically
- [ ] Fully qualified reference in code instead of explicit import

### Code organisation

- [ ] Declaration order violated
- [ ] Section separator missing or malformed

### Documentation

- [ ] Javadoc missing on a public class or public method
- [ ] `@param`, `@return`, or `@throws` missing where applicable

### Language

- [ ] Any identifier, comment, Javadoc, or exception message not in English

---

# Output format

List findings grouped by severity (Critical / Major / Minor).
For each finding: location (class + method), description, suggested fix.