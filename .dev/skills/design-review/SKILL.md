---
name: design-review
description: >
  Perform a design review for a new effect or class in denise4j.
  Use this skill before implementing any non-trivial class or when the user
  explicitly asks for a design validation.
---

# Design Review — denise4j

## When to run

- Before implementing a new effect or class
- When the user proposes a new algorithm
- When the user says "review the design", "is this a good approach",
  or "what do you think of this algorithm"

---

## Review checklist

Report only findings or recommendations.

### 1. Algorithm correctness

- [ ] The algorithm produces the expected pixel mapping for all valid inputs
- [ ] Edge cases are handled (boundary pixels, wrap-around, empty image)
- [ ] Integer arithmetic is used correctly (overflow, truncation, rounding)

### 2. Invariants

- [ ] Input constraints are clearly defined (null, dimensions, parameter ranges)
- [ ] Output invariants are clearly defined (dimensions, pixel range, image type)
- [ ] All invariants can be expressed using JML `@invariant`

### 3. Performance

- [ ] Per-pixel cost is acceptable
- [ ] No unnecessary allocations in the hot loop
- [ ] Bulk pixel access considered (`getRaster().getDataBuffer()`) where appropriate

### 4. In-place vs. out-of-place

- [ ] The choice is explicit and documented
- [ ] If in-place, the algorithm is correct when reading and writing the same buffer
- [ ] If out-of-place, the source image is not modified

### 5. Alternatives

For each relevant alternative algorithm:

- [ ] Alternative identified
- [ ] Advantages and disadvantages compared
- [ ] Reason for rejection documented

---

## Output format

List findings and recommendations. For each recommendation, explain the
tradeoff and suggest the preferred approach.