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

---

# `Stage`

    stage.image != null
    stage.image.getType() == BufferedImage.TYPE_INT_ARGB  // enforced by JML @invariant
    stage.getWidth() > 0
    stage.getHeight() > 0
    stage.getPixels() != null
    stage.getPixels().length == stage.getWidth() * stage.getHeight()
    // backgroundColor is a valid ARGB value (any int is valid)

---

# `PixelSource`

    pixelSource.getWidth() > 0
    pixelSource.getHeight() > 0

---

# `ImageSource`

    imageSource.image != null                         // image is always TYPE_INT_ARGB after construction
    imageSource.image.getType() == TYPE_INT_ARGB      // guaranteed: converted if needed in constructor
    imageSource.getWidth() > 0
    imageSource.getHeight() > 0
    imageSource.pixels != null                        // DataBufferInt array, cached at construction
    imageSource.pixels.length == getWidth() * getHeight()

---

# `TileSet`

    tileSet.image != null
    tileSet.tileWidth > 0
    tileSet.tileHeight > 0
    tileSet.tileCount > 0
    tileSet.tileCount == (image.getWidth() / tileWidth) * (image.getHeight() / tileHeight)

---

# `TileMap`

    tileMap.tileSet != null                                              // enforced by JML @invariant
    tileMap.cols > 0
    tileMap.rows > 0
    tileMap.edgePolicy != null
    tileMap.getWidth() == tileMap.cols * tileMap.tileSet.tileWidth
    tileMap.getHeight() == tileMap.rows * tileMap.tileSet.tileHeight
    // All tile indices in the grid are >= 0 and < tileSet.tileCount
    // isUnbounded() == (edgePolicy == WRAP)
    // getPixel(x, y) with out-of-bounds (x, y) on CLIP/FEED throws IndexOutOfBoundsException
    // The pipeline never calls getPixel out-of-bounds on a bounded source

---

# `ParamInt`

    // No range constraint by default — the effect validates at render time

---

# `ParamDouble`

    // No range constraint by default — the effect validates at render time

---

# `EffectPipeline`

    // After build(): the pipeline structure is immutable
    // At least one addSource() call before build()
    // The background color is carried by Stage, not by the pipeline

## Effects — `Transform` internal interface

Each effect method adds a `Transform` to the current layer's chain.
The following invariants apply to any `Transform` at render time:

    // prepare() is called exactly once per render per layer, before the pixel loop
    // srcX(x, y) and srcY(x, y) are called for every stage pixel
    // All coordinates are double — rounding is deferred to the bilinear sampler

## Effect — `scrollH`

    // offset: any integer value — negative, zero, positive
    // srcX = x + offset.get()
    // srcY = y (unchanged)
    // Result is always integer-valued — bilinear shortcut triggers (tx == 0.0 && ty == 0.0)

## Effect — `scrollV`

    // offset: any integer value
    // srcX = x (unchanged)
    // srcY = y + offset.get()
    // Result is always integer-valued — bilinear shortcut triggers

## Effect — `zoom`

    // factor > 0 required at render time (IllegalArgumentException if factor <= 0)
    // cx, cy: centre of zoom in stage coordinates; defaults to stageW/2, stageH/2
    // srcX = cx + (x - cx) / factor
    // srcY = cy + (y - cy) / factor
    // Bilinear interpolation for non-integer srcX/srcY
    // Edge clamping for bounded sources: x1 = min(x0 + 1, srcW - 1)

## Effect — `rotate`

    // angle: any double in radians (no range restriction)
    // cx, cy: centre of rotation in stage coordinates; defaults to stageW/2, stageH/2
    // cosA and sinA are precomputed in prepare(); not recomputed per pixel
    // srcX = cx + (x - cx) * cos(angle) + (y - cy) * sin(angle)
    // srcY = cy - (x - cx) * sin(angle) + (y - cy) * cos(angle)
    // Bilinear interpolation; same edge clamping as zoom

---

# `FrameDropPolicy`

Enum — pas d'invariants specifiques au-dela de la non-nullite.

---

# `FrameDropException`

`RuntimeException` standard — pas d'invariants specifiques.

---

# `FrameCallback`

`@FunctionalInterface` — pas d'invariants d'etat (interface sans champs).

---

# `StagePool`

    stagePool.getWidth() > 0
    stagePool.getHeight() > 0
    stagePool.getBufferCount() >= 2
    stagePool.getFrameDropPolicy() != null
    // Conservation : freeQueue.size() + readyQueue.size() + 1 (displayStage) == bufferCount
    // displayStage != null  (initialise a la construction avec un Stage vide)
    // Tous les stages ont les memes dimensions (width x height)

---

# `Orchestrator`

    // running ==> built
    // built ==> pipeline != null
    // built ==> stagePool != null
    // built ==> targetFps > 0
    // built ==> callback != null
    // deltaMs == 0 pour frameIndex == 0 (sentinel lastFrameStart = -1L)