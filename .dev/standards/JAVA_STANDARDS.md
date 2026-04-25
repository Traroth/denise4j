# Java Standards for denise4j

Coding standards and practices to follow for every Java class generated or
modified in this project.

---

## 1. Oracle guidelines compliance

### Source file structure

Elements in a Java source file must appear in the following order:

1. Beginning file comment (see below)
2. `package` statement
3. `import` statements (explicit, see below)
4. Class or interface declaration, with Javadoc

### Beginning file comment

Every source file must begin with a C-style comment containing:
- The class name
- The version
- The copyright notice (LGPL license)

Example:
```
/*
 * ClassName.java
 *
 * Version 1.0-SNAPSHOT
 *
 * denise4j - A Java demoscene-inspired graphics effects library
 * Copyright (C) 2026  Dufrenoy
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see
 * <https://www.gnu.org/licenses/>.
 */
```

### Imports

- Wildcard imports (`import java.awt.*`) are forbidden
- Each imported class must have its own `import` statement
- Imports must be sorted alphabetically within each group
- Fully qualified references in code must be replaced by explicit imports

### Declaration order within a class

1. Static variables (`public`, then `protected`, then package, then `private`)
2. Instance variables (`public`, then `protected`, then package, then `private`)
3. Constructors
4. Methods (grouped by functionality, not by visibility)

### Section separators

Custom decorative separators are allowed and encouraged for readability:

```java
// ─── Section name ─────────────────────────────────────────────────────────────
```

---

## 2. Javadoc

- All public classes must have a class-level Javadoc comment
- All public methods must have a Javadoc comment
- `@Override` methods may omit Javadoc if their behavior is identical to the
  parent contract, but must have one if the behavior differs
- Parameters (`@param`), return values (`@return`), and exceptions (`@throws`)
  must be documented

---

## 3. Null handling

Prefer `Optional<T>` over `null` as a return type, except in the following cases:

- **Interface contracts** — never change the return type of a method imposed
  by an interface
- **Method parameters** — never use `Optional` as a parameter type
- **Instance fields** — use `null` for optional fields
- **Collections** — never use `Optional<Collection<T>>`; return an empty
  collection instead

When a method is constrained to return `null`, document it with
`@return ... or {@code null} if not found`.

---

## 4. Tests

### Two levels of testing

Each class has two levels of tests:

#### Level 1 — Black-box tests (`ClassNameBlackBoxTest`)

- Tests the public contract only
- No knowledge of internal implementation is used
- Must be writable without reading the source code
- Cover: nominal behaviour, boundary conditions, error cases (expected exceptions)
- Named `ClassNameBlackBoxTest`

#### Level 2 — White-box tests (`ClassNameWhiteBoxTest`)

- Tests technically risky internal areas
- Each test must have a comment explaining *why* it targets a specific
  internal point (e.g. wrap-around, integer overflow, rounding boundary)
- White-box tests must not duplicate black-box tests

### General principles

- Systematically cover: nominal case, boundary cases (null image, zero-size
  image, single pixel), error cases (expected exception)
- White-box tests must not duplicate black-box tests — they target only what
  cannot be observed through the public API

---

## 5. Versioning

The version in each source file header must match the project version in
`pom.xml`. When the project version is bumped, all file headers must be
updated accordingly.

Project versioning follows **Semantic Versioning** (semver):

- **Patch (x.y.Z)** — bug fix, Javadoc improvement, internal refactoring with
  no behavioral change
- **Minor (x.Y.0)** — new public method, new effect, backward-compatible change
- **Major (X.0.0)** — method signature change, method removal, breaking change

---

## 6. Static analysis

### Claude-assisted analysis

After every class generation or significant modification, explicitly ask Claude
to perform a static analysis of the produced code. Claude will report issues
classified by severity:

- **Critical** — bugs, contract violations, broken invariants
- **Major** — performance issues, missing null checks, incorrect pixel handling
- **Minor** — style issues, Javadoc gaps, guideline violations

Claude also verifies that all of the following are written in English:
- Comments and Javadoc
- Class, method, field, and variable names
- Exception messages

---

## 7. Naming conventions

### Exception parameters in catch blocks

Exception parameters in `catch` blocks must be named using the initials of the
exception type in lowercase:

| Exception type | Parameter name |
|---|---|
| `IOException` | `ioe` |
| `NullPointerException` | `npe` |
| `IllegalArgumentException` | `iae` |
| `IllegalStateException` | `ise` |
| `ArrayIndexOutOfBoundsException` | `aioobe` |

For generic or unknown exception types, `e` is acceptable.

---

## 8. JML annotations

### Placement relative to Java annotations

JML specification comments (`//@ requires`, `//@ ensures`, `//@ also`, etc.)
must be placed **after** the Javadoc block and **before** any Java annotations
(`@Override`, `@SuppressWarnings`, etc.).

Correct:

```java
/**
 * Applies the effect to the given image.
 *
 * @param image the source image
 */
//@ requires image != null;
//@ ensures \result != null;
@Override
public BufferedImage apply(BufferedImage image) {
```

### `also` keyword on overriding methods

When a method overrides a parent method, its JML specification must begin
with `//@ also`.

```java
//@ also
//@ requires image != null;
//@ ensures \result != null;
@Override
public BufferedImage apply(BufferedImage image) {
```