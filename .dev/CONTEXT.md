# Context for denise4j

This is the entry point for Claude (or any other AI coding agent) working on
this project. At the beginning of each session, read all files listed below
before generating or modifying any code.

## Files to read

- `.dev/standards/JAVA_STANDARDS.md` — coding standards and conventions for
  all Java code in this project
- `.dev/backlog/BACKLOG.md` — pending tasks and known issues for all classes
- `.dev/design/ARCHITECTURE.md` — key design decisions and rationale for each class
- `.dev/WORKFLOW.md` — required order of operations for any class creation or
  modification

## Skills

Skills encode recurring workflows. Read the relevant skill file before starting
the corresponding task.

| Task                                                        | Skill                                              |
|-------------------------------------------------------------|----------------------------------------------------|
| Generate or significantly modify a class                    | `.dev/skills/static-analysis/SKILL.md`             |
| Create a new class                                          | `.dev/skills/new-class/SKILL.md`                   |
| Before implementing a new effect or class                   | `.dev/skills/design-review/SKILL.md`               |
| After writing tests or when coverage seems insufficient     | `.dev/skills/test-coverage-review/SKILL.md`        |
| Writing JML contracts (step 2 or retrofitting)              | `.dev/skills/jml-design/SKILL.md`                  |
| Generating or reviewing tests from JML specs                | `.dev/skills/jml-test-generation/SKILL.md`         |
| After implementation (JML → Code verification)             | `.dev/skills/jml-conformance-check/SKILL.md`       |
| After implementation is stable (Code → JML verification)   | `.dev/skills/jml-completeness-check/SKILL.md`      |
| End of session                                              | `.dev/skills/update-backlog/SKILL.md`              |

## Project overview

`denise4j` is a Java graphics library inspired by demoscene effects from the
80s/90s. It is licensed under the GNU Lesser General Public License v3.

It exploits Java's image classes (`BufferedImage`, etc.) to produce graphical
effects and animations via direct pixel manipulation.

### Planned effects

- **Scrolling** — horizontal, vertical, diagonal
- **Zoom** — fixed-point and animated
- **Rotation** — fixed-point and animated
- **Deformation** — distortion grids, wave effects
- **Palette** — palette cycling, fading, remapping
- **Compositing** — blending, masking, overlaying images

### Build

- **Java version:** 11
- **Build tool:** Maven

### Package

`fr.dufrenoy.imagefx`

### Repository structure

```
src/
  main/java/fr/dufrenoy/imagefx/
  test/java/fr/dufrenoy/imagefx/
.dev/
  CONTEXT.md
  WORKFLOW.md
  standards/
    JAVA_STANDARDS.md
  backlog/
    BACKLOG.md
  design/
    ARCHITECTURE.md
    INVARIANTS.md
  skills/
    static-analysis/SKILL.md
    new-class/SKILL.md
    design-review/SKILL.md
    test-coverage-review/SKILL.md
    jml-design/SKILL.md
    jml-conformance-check/SKILL.md
    jml-completeness-check/SKILL.md
    jml-test-generation/SKILL.md
    update-backlog/SKILL.md
```