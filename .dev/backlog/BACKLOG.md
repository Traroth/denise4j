# Backlog — denise4j

Pending tasks and known issues, grouped by class. Update this file at the end
of each session.

---

## Project-wide

- [x] Set up Maven project structure (`src/main`, `src/test`, dependencies)
- [x] Configure `pom.xml` with JUnit and Maven Surefire plugin
- [x] Define architecture (pipeline, sources, effects, rendering)

---

## Stage

- [x] Design discussion (step 1)
- [x] Stub class + JML specifications (step 2)
- [x] BlackBoxTest
- [x] Implementation
- [x] WhiteBoxTest

## PixelSource

- [x] Design discussion (step 1)
- [x] Stub interface + JML specifications (step 2)
- [x] Implementation (interface — isUnbounded() default method ajoutee)
- [x] JML fix: requires conditionnel sur !isUnbounded() pour getPixel (session 2026-04-27)
- [x] JML completeness: invariants getWidth/getHeight ajoutes a l'interface (session 2026-04-27)

## ImageSource

- [x] Design discussion (step 1)
- [x] Stub class + JML specifications (step 2)
- [x] BlackBoxTest
- [x] Implementation
- [x] Perf fix: getPixel via DataBufferInt direct array access (etait getRGB — session 2026-04-27)
- [x] JML completeness: invariants DataBufferInt documentes (session 2026-04-27)

## TileSet

- [x] Design discussion (step 1)
- [x] Stub class + JML specifications (step 2)
- [x] BlackBoxTest
- [x] Implementation
- [x] WhiteBoxTest
- [x] JML completeness: @ensures sur getTilePixel (session 2026-04-27)

## TileMap

- [x] Design discussion (step 1)
- [x] Stub class + JML specifications (step 2)
- [x] BlackBoxTest
- [x] Implementation (EdgePolicy: WRAP, CLIP, FEED)
- [x] WhiteBoxTest
- [x] Critical fix: Javadoc CLIP/FEED corrige (retournait "transparent" — maintenant lance IOOBE — session 2026-04-27)
- [x] JML completeness: invariant getTileSet() != null ajoute; @ensures sur getPixel (session 2026-04-27)

## ParamInt

- [x] Design discussion (step 1)
- [x] Stub class + JML specifications (step 2)
- [x] BlackBoxTest
- [x] Implementation

## ParamDouble

- [x] Design discussion (step 1)
- [x] Stub class + JML specifications (step 2)
- [x] BlackBoxTest
- [x] Implementation

## EffectPipeline

- [x] Design discussion (step 1)
- [x] Stub class + JML specifications (step 2)
- [x] BlackBoxTest
- [x] Implementation (addSource, transparentColor, fuseLayer, build, render)
- [x] WhiteBoxTest

---

## Effets — scrollH / scrollV

- [x] Design discussion
- [x] BlackBoxTest
- [x] Implementation (transform interne, int offset, coordonnees negatives)
- [x] WhiteBoxTest (ordre des transforms, prepare par render)
- [x] Static analysis + conformance + completeness (session 2026-04-27)
- [x] Documentation (ARCHITECTURE.md, INVARIANTS.md)

## Effets — zoom

- [x] Design discussion
- [x] BlackBoxTest
- [x] Implementation (bilineaire, centre stage ou explicite, factor <= 0 interdit)
- [x] WhiteBoxTest (clampage bord, chemin exact tx=0/ty=0)
- [x] Static analysis + conformance + completeness (session 2026-04-27)
- [x] Documentation (ARCHITECTURE.md, INVARIANTS.md)

## Effets — rotate

- [x] Design discussion
- [x] BlackBoxTest
- [x] Implementation (horaire, bilineaire, cos/sin precalcules par render, centre configurable)
- [x] WhiteBoxTest
- [x] Static analysis + conformance + completeness (session 2026-04-27)
- [x] Documentation (ARCHITECTURE.md, INVARIANTS.md)

---

## Deformation (a venir)

- [ ] Design discussion (wave, distortion, tunnel, plasma...)
- [ ] Implementation
- [ ] BlackBoxTest
- [ ] WhiteBoxTest

## Palette (a venir)

- [ ] Design discussion (cycling, fading, remapping)
- [ ] Implementation
- [ ] BlackBoxTest
- [ ] WhiteBoxTest

## Compositing avance (a venir)

- [ ] Design discussion (blending, masking, overlay)
- [ ] Implementation
- [ ] BlackBoxTest
- [ ] WhiteBoxTest

---

## FrameDropPolicy

- [x] Design discussion (step 1)
- [x] Stub enum + JML specifications (step 2)
- [x] Implementation
- [x] BlackBoxTest (via StagePoolBlackBoxTest)
- [x] Static analysis (step 9)

## FrameDropException

- [x] Design discussion (step 1)
- [x] Stub class + JML specifications (step 2)
- [x] Implementation
- [x] BlackBoxTest (via StagePoolBlackBoxTest)
- [x] Static analysis (step 9)

## FrameCallback

- [x] Design discussion (step 1)
- [x] Stub interface + JML specifications (step 2)
- [x] Implementation
- [x] BlackBoxTest (via OrchestratorBlackBoxTest)
- [x] Static analysis (step 9)

## StagePool

- [x] Design discussion (step 1)
- [x] Stub class + JML specifications (step 2)
- [x] BlackBoxTest
- [x] Implementation
- [x] WhiteBoxTest (conservation N cycles, WAIT interrupt, displayStage freed)
- [x] Static analysis (step 9)
- [x] JML conformance check (step 10) — aucune violation
- [x] JML completeness check (step 11) — @ensures ajoute sur present()
- [x] Test coverage review (step 12) — 96% instructions, 95% branches ; residuel acceptable
- [x] Update ARCHITECTURE.md + INVARIANTS.md (step 4)
- [x] Update BACKLOG.md (step 5)

## Orchestrator

- [x] Design discussion (step 1)
- [x] Stub class + JML specifications (step 2)
- [x] BlackBoxTest
- [x] Implementation (boucle de frames, double-buffering, timing)
- [x] WhiteBoxTest (thread name, stop() sur render bloque en acquire)
- [x] Static analysis (step 9)
- [x] JML conformance check (step 10) — aucune violation
- [x] JML completeness check (step 11) — @ensures ajoutes sur setters et start()
- [x] Test coverage review (step 12) — 99% instructions, 96% branches ; residuel acceptable
- [x] Update ARCHITECTURE.md + INVARIANTS.md (step 4)
- [x] Update BACKLOG.md (step 5)

---

## Done