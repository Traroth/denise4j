---
name: test-coverage-review
description: >
  Review the test suite of a class in denise4j and identify missing test cases.
  Use this skill after writing tests or when the user asks for a coverage review.
---

# Test Coverage Review — denise4j

## When to run

- After writing `BlackBoxTest` and `WhiteBoxTest`
- Before marking a backlog task as done
- When the user says "review the tests" or "is the test coverage sufficient"

---

## Coverage checklist

### 1. Public API coverage

- [ ] Every public method has at least one test
- [ ] Both nominal and error cases are tested
- [ ] Null image input tested (expect exception)

### 2. Boundary cases

- [ ] Minimal image (1×1 pixel)
- [ ] Parameters at minimum and maximum valid values
- [ ] Parameters at boundary (e.g. scroll = 0, scroll = width - 1)

### 3. Pixel correctness

- [ ] Specific pixels are verified, not just dimensions
- [ ] Wrap-around behaviour verified
- [ ] Alpha channel verified if applicable

### 4. White-box coverage

- [ ] Coordinate overflow scenarios tested
- [ ] Internal loop boundaries tested (first row, last row, first column, last column)

---

## Output format

List findings grouped by category. For each missing test, suggest
a test method signature and the scenario it should cover.