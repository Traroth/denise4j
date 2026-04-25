---
name: new-class
description: >
  Scaffold a new effect class in denise4j: generate the source file, the test
  classes, update BACKLOG.md and ARCHITECTURE.md, and run a static analysis.
  Use this skill whenever the user asks to create, implement, or scaffold a new
  effect or class — even if they just say "let's add X" or "I want to start on Y".
---

# New Class — denise4j

## Before writing any code

Read the following files in full:

1. `.dev/WORKFLOW.md` — the required order of operations; follow it strictly
2. `.dev/standards/JAVA_STANDARDS.md` — coding standards
3. `.dev/design/ARCHITECTURE.md` — existing design decisions
4. `.dev/backlog/BACKLOG.md` — check if this class is already listed

---

## Procedure

Follow the order defined in `.dev/WORKFLOW.md`:

1. Clarify the design
2. Stub class + JML (step 2)
3. Update `ARCHITECTURE.md` and `INVARIANTS.md` (step 4)
4. Update `BACKLOG.md` (step 5)
5. Write `BlackBoxTest` (step 6)
6. Implement (step 7)
7. Write `WhiteBoxTest` (step 8)
8. Run `static-analysis` (step 9)
9. Run `jml-conformance-check` (step 10)
10. Run `jml-completeness-check` (step 11)
11. Run `test-coverage-review` (step 12)

### 1. Clarify the design (before coding)

Ask the user — or infer from context — the following:

- What is the effect? What are inputs and outputs?
- Does the effect operate in-place or produce a new image?
- What are the parameters, and what are their valid ranges?
- What is the expected pixel-level behaviour at boundaries?
- What are the performance constraints?

Do not proceed to code until the design is clear.

### 2. Generate the source file

Location: `src/main/java/fr/dufrenoy/imagefx/ClassName.java`

Required structure (in order):

1. **File header** — C-style comment with class name, version (match `pom.xml`),
   and full LGPL v3 notice
2. **`package` statement**
3. **`import` statements** — explicit, sorted alphabetically, no wildcards
4. **Class Javadoc** — describe the effect, its invariants, expected performance
5. **Class declaration**
6. **Body** — static fields, instance fields, constructors, methods
   (grouped by functionality, separated by `// ─── Section ───...` decorators)

### 3. Generate the two test classes

#### `ClassNameBlackBoxTest`

Location: `src/test/java/fr/dufrenoy/imagefx/ClassNameBlackBoxTest.java`

- Tests based solely on the public contract
- Cover: nominal case, boundary cases (null image, minimal image), error cases

#### `ClassNameWhiteBoxTest`

Location: `src/test/java/fr/dufrenoy/imagefx/ClassNameWhiteBoxTest.java`

- Tests targeting technically risky pixel-level areas
- Each test must have a comment explaining *why* it targets that scenario
  (e.g. wrap-around at image boundary, integer overflow in coordinate computation)

### 4. Run static analysis

After generating the source file, apply the `static-analysis` skill.

### 5. Update ARCHITECTURE.md

Add a new section for the class.

### 6. Update BACKLOG.md

Add a new `## ClassName` section and move completed items to `### Done`.

---

## Checklist before handing off

- [ ] File header present and version matches `pom.xml`
- [ ] All imports explicit and sorted
- [ ] All public methods have Javadoc with `@param`, `@return`, `@throws`
- [ ] JML contracts present on all public methods
- [ ] Static analysis run and findings reported
- [ ] `ARCHITECTURE.md` updated
- [ ] `BACKLOG.md` updated