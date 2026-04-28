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

## Exemples (fr.dufrenoy.imagefx.examples)

- [x] FleursDemoExample — zoom + rotation de fleurs.jpg en plein écran (session 2026-04-28)
- [x] PaysageDemoExample — scrolling Lissajous multidirectionnel sur paysage_montagne.jpg (session 2026-04-28)
- [x] ShadowDemo — parallax 5 couches inspiré Shadow of the Beast : ciel dégradé+lune, nuages, aiguilles rocheuses ocre, masses rocheuses sombres, menhirs ardoise (session 2026-04-29)

---

## Optimisations (session 2026-04-29)

- [x] EffectPipeline — Transform interface : `apply(double[] coords)` remplace `srcX/srcY` séparés (1 dispatch virtuel au lieu de 2, RotateTransform calcule dx/dy une seule fois)
- [x] EffectPipeline — bilinéaire virgule fixe entière (poids ×256, shift) — supprime 16 multiplications double + 4 Math.round() par pixel bilinéaire
- [x] EffectPipeline — boucle y parallélisée par layer via `IntStream.range(0, h).parallel()` — thread-safe : chaque tâche écrit pixels[y*w..y*w+w-1] exclusivement, sources read-only
- [x] EffectPipeline — `Layer.transformsArr` (Transform[]) figé à build() — évite l'overhead ArrayList iterator en hot loop
- [x] Orchestrator — drift correction : cible temporelle absolue (`targetTime += frameNanos`) au lieu de `frameNanos - elapsed` — compense les overruns sans dériver sur la durée
- [x] White-box tests : bilinéaire précision à 0.5 et 2D midpoint, determinisme du rendu parallèle, dx/dy pré-sauvés dans RotateTransform.apply()

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