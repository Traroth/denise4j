---
name: update-backlog
description: >
  Update BACKLOG.md at the end of a working session in denise4j.
  Trigger this skill when the user says "update the backlog", "end of session",
  "wrap up", or "what did we do". Also run it automatically after completing
  any backlog task.
---

# Update Backlog — denise4j

## When to run

- At the end of every working session
- After completing, partially completing, or abandoning a backlog task
- After discovering a new issue worth tracking

---

## Procedure

### 1. Identify changes from the session

Review the conversation and extract:

- Tasks **fully completed** → move to `### Done` under the correct class, using `[x]`
- Tasks **partially completed** → keep with `[ ]`, add `(in progress — [what remains])`
- Tasks **abandoned or on hold** → keep with `[ ]`, add `— on hold` with a short reason
- **New tasks or issues discovered** → add under the correct class with `[ ]`
- **New design decisions** → belong in `ARCHITECTURE.md`, not the backlog; flag them

### 2. Write the update

```markdown
- [ ] Task description — optional note
- [x] Completed task description
```

- One task per line
- Short, imperative phrasing ("Create", "Fix", "Implement", "Add")
- If a task has a dependency, append it after ` — `

### 3. Present the diff to the user

Before writing, show the user what you are marking as done, adding, or modifying.
Ask for confirmation if anything is ambiguous.

---

## Format reminder

```markdown
# Backlog — denise4j

## Project-wide
...

## ClassName
...

---

## Done

### ClassName
- [x] ...
```

Preserve all existing content. Only add, tick, or annotate — never delete.