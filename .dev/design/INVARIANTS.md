# INVARIANTS.md — denise4j

This document defines the structural invariants expected from the classes
implemented in **denise4j**.

An invariant is a condition that must **always hold for any observable
state** of a class. Public methods must preserve these invariants.

Whenever possible, invariants should also be expressed in the code using
**JML `@invariant` annotations**.

---

# General principles

All effect classes in the library must respect the following principles:

- Input images passed to effect methods must not be null
- Output images must have the same type as the input unless explicitly documented
- Pixel values must remain within the valid range for the image type
- Effect parameters must remain within their valid domain at all times

---

# General image invariants

For any image processed by an effect:

    image != null
    image.getWidth() > 0
    image.getHeight() > 0

For pixel values (TYPE_INT_ARGB or TYPE_INT_RGB):

    0 <= pixel_component <= 255    (for each R, G, B, A component)

---

# When adding a new class

Whenever a new class is introduced:

1. Identify its core invariants (parameter ranges, image constraints)
2. Add them to the class using JML annotations
3. Document any non-obvious invariants in this file
4. Ensure all methods preserve them

---

*(No classes implemented yet. Add a section here for each new class.)*