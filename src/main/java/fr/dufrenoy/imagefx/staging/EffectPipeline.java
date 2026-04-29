/*
 * EffectPipeline.java
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
package fr.dufrenoy.imagefx.staging;

import fr.dufrenoy.imagefx.source.PixelSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * A chain of graphical effects rendered into a {@link Stage}.
 *
 * <p>The pipeline is built once with a fluent API and reused across
 * frames. Its structure (sources, effects, fusion points) is fixed
 * after {@link #build()}; only the {@link ParamInt} and
 * {@link ParamDouble} values change between frames.</p>
 *
 * <h2>Construction model</h2>
 *
 * <p>The pipeline is composed of one or more layers. Each layer is
 * opened by {@link #addSource(PixelSource)} and may carry its own
 * chain of effects (scroll, zoom, rotation, etc.) plus an optional
 * {@linkplain #transparentColor(int) color key} for compositing onto
 * the previous layers.</p>
 *
 * <p>{@link #fuseLayer()} fuses the accumulated layers; subsequent
 * effects then apply to the fused result, until the next
 * {@link #addSource(PixelSource)} opens a new layer.</p>
 *
 * <p>The background color is carried by the {@link Stage} itself, not
 * by the pipeline. {@link #render(Stage)} clears the stage with
 * {@link Stage#getBackgroundColor()} before compositing the layers.</p>
 *
 * <h2>Typical usage</h2>
 *
 * <pre>{@code
 * EffectPipeline pipeline = new EffectPipeline()
 *     .addSource(background)
 *         .scrollH(bgScroll)
 *     .addSource(sprites)
 *         .transparentColor(0xFF00FF)
 *         .scrollH(fgScroll)
 *         .zoom(fgZoom)
 *     .build();
 *
 * // Each frame:
 * bgScroll.add(1);
 * pipeline.render(stage);
 * }</pre>
 */
public class EffectPipeline {

    /*@
      @ public invariant !isBuilt() || hasAtLeastOneSource();
      @*/

    // ─── Inner transform representation ─────────────────────────────────────────

    private interface Transform {
        /** Called once per render call before the pixel loop. */
        default void prepare(int stageW, int stageH) {}

        /**
         * Applies this transform in-place on {@code coords}.
         * {@code coords[0]} is srcX, {@code coords[1]} is srcY.
         * Implementations must read both values before writing either.
         */
        void apply(double[] coords);
    }

    private static final class ScrollHTransform implements Transform {
        final ParamInt offset;
        ScrollHTransform(ParamInt offset) { this.offset = offset; }
        public void apply(double[] c) { c[0] += offset.get(); }
    }

    private static final class ScrollVTransform implements Transform {
        final ParamInt offset;
        ScrollVTransform(ParamInt offset) { this.offset = offset; }
        public void apply(double[] c) { c[1] += offset.get(); }
    }

    private static final class ScrollHDoubleTransform implements Transform {
        final ParamDouble offset;
        ScrollHDoubleTransform(ParamDouble offset) { this.offset = offset; }
        public void apply(double[] c) { c[0] += offset.get(); }
    }

    private static final class ScrollVDoubleTransform implements Transform {
        final ParamDouble offset;
        ScrollVDoubleTransform(ParamDouble offset) { this.offset = offset; }
        public void apply(double[] c) { c[1] += offset.get(); }
    }

    private static final class ZoomTransform implements Transform {
        final ParamDouble factor;
        final ParamDouble cx, cy; // null = stage centre
        double factorVal, cxVal, cyVal;

        ZoomTransform(ParamDouble factor, ParamDouble cx, ParamDouble cy) {
            this.factor = factor; this.cx = cx; this.cy = cy;
        }

        public void prepare(int stageW, int stageH) {
            factorVal = factor.get();
            if (factorVal <= 0)
                throw new IllegalArgumentException(
                        "zoom factor must be strictly positive, got: " + factorVal);
            cxVal = cx != null ? cx.get() : stageW / 2.0;
            cyVal = cy != null ? cy.get() : stageH / 2.0;
        }

        // srcX and srcY are independent: updating c[0] first is safe.
        public void apply(double[] c) {
            c[0] = cxVal + (c[0] - cxVal) / factorVal;
            c[1] = cyVal + (c[1] - cyVal) / factorVal;
        }
    }

    private static final class RotateTransform implements Transform {
        final ParamDouble angle;
        final ParamDouble cx, cy; // null = stage centre
        double cosA, sinA, cxVal, cyVal;

        RotateTransform(ParamDouble angle, ParamDouble cx, ParamDouble cy) {
            this.angle = angle; this.cx = cx; this.cy = cy;
        }

        public void prepare(int stageW, int stageH) {
            double a = angle.get();
            cosA = Math.cos(a);
            sinA = Math.sin(a);
            cxVal = cx != null ? cx.get() : stageW / 2.0;
            cyVal = cy != null ? cy.get() : stageH / 2.0;
        }

        // dx/dy are saved before modifying c[0]: both outputs use original input.
        public void apply(double[] c) {
            double dx = c[0] - cxVal;
            double dy = c[1] - cyVal;
            c[0] = cxVal + dx * cosA + dy * sinA;
            c[1] = cyVal - dx * sinA + dy * cosA;
        }
    }

    // ─── Inner layer representation ──────────────────────────────────────────────

    private static class Layer {
        final PixelSource source;
        int transparentColor;
        boolean hasTransparentColor;
        final List<Transform> transforms = new ArrayList<>();
        Transform[] transformsArr; // set at build(), used during render()

        Layer(PixelSource source) {
            this.source = source;
        }
    }

    // ─── Instance variables ──────────────────────────────────────────────────────

    private boolean built;
    private boolean hasSource;
    private final List<Layer> layers;
    private Layer currentLayer;

    // ─── Constructors ────────────────────────────────────────────────────────────

    /**
     * Creates a new, empty pipeline.
     *
     * <p>Sources and effects must be added before calling
     * {@link #build()}.</p>
     */
    //@ ensures !isBuilt();
    public EffectPipeline() {
        this.built = false;
        this.hasSource = false;
        this.layers = new ArrayList<>();
        this.currentLayer = null;
    }

    // ─── Guards ──────────────────────────────────────────────────────────────────

    private void requireNotBuilt() {
        if (built) throw new IllegalStateException("Pipeline has already been built");
    }

    private void requireHasSource() {
        if (!hasSource) throw new IllegalStateException("No source has been added yet");
    }

    // ─── Layer construction ──────────────────────────────────────────────────────

    /**
     * Opens a new layer backed by the given pixel source.
     *
     * <p>Subsequent effect calls (e.g. {@link #scrollH(ParamInt)},
     * {@link #zoom(ParamDouble)}) apply to this newly opened layer
     * until either {@link #fuseLayer()} or another
     * {@link #addSource(PixelSource)} call.</p>
     *
     * @param source the pixel source to add as a new layer
     * @return this pipeline, for chaining
     * @throws NullPointerException  if {@code source} is {@code null}
     * @throws IllegalStateException if the pipeline has already been built
     */
    //@ requires source != null;
    //@ requires !isBuilt();
    //@ ensures hasAtLeastOneSource();
    //@ ensures \result == this;
    public EffectPipeline addSource(PixelSource source) {
        requireNotBuilt();
        if (source == null) throw new NullPointerException("source must not be null");
        currentLayer = new Layer(source);
        layers.add(currentLayer);
        hasSource = true;
        return this;
    }

    /**
     * Sets the transparent color (color key) of the current layer.
     *
     * <p>Pixels matching this ARGB value are treated as transparent
     * when the layer is composited over the previous ones.</p>
     *
     * @param argb the transparent color in ARGB format
     * @return this pipeline, for chaining
     * @throws IllegalStateException if no source has been opened yet,
     *         or if the pipeline has already been built
     */
    //@ requires !isBuilt();
    //@ requires hasAtLeastOneSource();
    //@ ensures \result == this;
    public EffectPipeline transparentColor(int argb) {
        requireNotBuilt();
        requireHasSource();
        currentLayer.transparentColor = argb;
        currentLayer.hasTransparentColor = true;
        return this;
    }

    /**
     * Fuses the layers accumulated so far into a single intermediate
     * result. Subsequent effects apply to the fused result until the
     * next {@link #addSource(PixelSource)} call.
     *
     * @return this pipeline, for chaining
     * @throws IllegalStateException if no source has been opened yet,
     *         or if the pipeline has already been built
     */
    //@ requires !isBuilt();
    //@ requires hasAtLeastOneSource();
    //@ ensures \result == this;
    public EffectPipeline fuseLayer() {
        requireNotBuilt();
        requireHasSource();
        return this;
    }

    // ─── Effects ─────────────────────────────────────────────────────────────────

    /**
     * Adds a horizontal scrolling effect to the current layer.
     *
     * <p>Stage pixel {@code (x, y)} reads from source pixel
     * {@code (x + offset.get(), y)}. Positive offset moves content
     * leftward on screen; negative offset moves it rightward.</p>
     *
     * @param offset the horizontal offset parameter (in pixels)
     * @return this pipeline, for chaining
     * @throws NullPointerException  if {@code offset} is {@code null}
     * @throws IllegalStateException if no source has been opened yet,
     *         or if the pipeline has already been built
     */
    //@ requires offset != null;
    //@ requires !isBuilt();
    //@ requires hasAtLeastOneSource();
    //@ ensures \result == this;
    public EffectPipeline scrollH(ParamInt offset) {
        requireNotBuilt();
        requireHasSource();
        if (offset == null) throw new NullPointerException("offset must not be null");
        currentLayer.transforms.add(new ScrollHTransform(offset));
        return this;
    }

    /**
     * Adds a vertical scrolling effect to the current layer.
     *
     * <p>Stage pixel {@code (x, y)} reads from source pixel
     * {@code (x, y + offset.get())}. Positive offset moves content
     * upward on screen; negative offset moves it downward.</p>
     *
     * @param offset the vertical offset parameter (in pixels)
     * @return this pipeline, for chaining
     * @throws NullPointerException  if {@code offset} is {@code null}
     * @throws IllegalStateException if no source has been opened yet,
     *         or if the pipeline has already been built
     */
    //@ requires offset != null;
    //@ requires !isBuilt();
    //@ requires hasAtLeastOneSource();
    //@ ensures \result == this;
    public EffectPipeline scrollV(ParamInt offset) {
        requireNotBuilt();
        requireHasSource();
        if (offset == null) throw new NullPointerException("offset must not be null");
        currentLayer.transforms.add(new ScrollVTransform(offset));
        return this;
    }

    /**
     * Adds a horizontal scrolling effect with sub-pixel precision to the
     * current layer.
     *
     * <p>Stage pixel {@code (x, y)} reads from source pixel
     * {@code (x + offset.get(), y)}. The fractional part of the offset
     * is resolved by bilinear interpolation, enabling smooth motion for
     * slow-moving layers without integer quantisation artefacts.</p>
     *
     * @param offset the horizontal offset parameter (in pixels, may be fractional)
     * @return this pipeline, for chaining
     * @throws NullPointerException  if {@code offset} is {@code null}
     * @throws IllegalStateException if no source has been opened yet,
     *         or if the pipeline has already been built
     */
    //@ requires offset != null;
    //@ requires !isBuilt();
    //@ requires hasAtLeastOneSource();
    //@ ensures \result == this;
    public EffectPipeline scrollH(ParamDouble offset) {
        requireNotBuilt();
        requireHasSource();
        if (offset == null) throw new NullPointerException("offset must not be null");
        currentLayer.transforms.add(new ScrollHDoubleTransform(offset));
        return this;
    }

    /**
     * Adds a vertical scrolling effect with sub-pixel precision to the
     * current layer.
     *
     * <p>Stage pixel {@code (x, y)} reads from source pixel
     * {@code (x, y + offset.get())}. The fractional part of the offset
     * is resolved by bilinear interpolation, enabling smooth motion for
     * slow-moving layers without integer quantisation artefacts.</p>
     *
     * @param offset the vertical offset parameter (in pixels, may be fractional)
     * @return this pipeline, for chaining
     * @throws NullPointerException  if {@code offset} is {@code null}
     * @throws IllegalStateException if no source has been opened yet,
     *         or if the pipeline has already been built
     */
    //@ requires offset != null;
    //@ requires !isBuilt();
    //@ requires hasAtLeastOneSource();
    //@ ensures \result == this;
    public EffectPipeline scrollV(ParamDouble offset) {
        requireNotBuilt();
        requireHasSource();
        if (offset == null) throw new NullPointerException("offset must not be null");
        currentLayer.transforms.add(new ScrollVDoubleTransform(offset));
        return this;
    }

    /**
     * Adds a zoom effect to the current layer, centred on the middle of
     * the stage.
     *
     * <p>Pixels are sampled with bilinear interpolation.
     * {@code factor > 1} magnifies the content; {@code 0 < factor < 1}
     * minifies it. A non-positive factor throws
     * {@link IllegalArgumentException} at render time.</p>
     *
     * @param factor the zoom factor parameter (must be {@code > 0} at render time)
     * @return this pipeline, for chaining
     * @throws NullPointerException  if {@code factor} is {@code null}
     * @throws IllegalStateException if no source has been opened yet,
     *         or if the pipeline has already been built
     */
    //@ requires factor != null;
    //@ requires !isBuilt();
    //@ requires hasAtLeastOneSource();
    //@ ensures \result == this;
    public EffectPipeline zoom(ParamDouble factor) {
        requireNotBuilt();
        requireHasSource();
        if (factor == null) throw new NullPointerException("factor must not be null");
        currentLayer.transforms.add(new ZoomTransform(factor, null, null));
        return this;
    }

    /**
     * Adds a zoom effect to the current layer, centred on
     * {@code (cx.get(), cy.get())} in stage coordinates.
     *
     * @param factor the zoom factor parameter (must be {@code > 0} at render time)
     * @param cx     the x coordinate of the zoom centre
     * @param cy     the y coordinate of the zoom centre
     * @return this pipeline, for chaining
     * @throws NullPointerException  if any parameter is {@code null}
     * @throws IllegalStateException if no source has been opened yet,
     *         or if the pipeline has already been built
     */
    //@ requires factor != null && cx != null && cy != null;
    //@ requires !isBuilt();
    //@ requires hasAtLeastOneSource();
    //@ ensures \result == this;
    public EffectPipeline zoom(ParamDouble factor, ParamDouble cx, ParamDouble cy) {
        requireNotBuilt();
        requireHasSource();
        if (factor == null) throw new NullPointerException("factor must not be null");
        if (cx     == null) throw new NullPointerException("cx must not be null");
        if (cy     == null) throw new NullPointerException("cy must not be null");
        currentLayer.transforms.add(new ZoomTransform(factor, cx, cy));
        return this;
    }

    /**
     * Adds a clockwise rotation effect to the current layer, centred on
     * the middle of the stage.
     *
     * <p>Pixels are sampled with bilinear interpolation.</p>
     *
     * @param angle the rotation angle parameter (in radians, clockwise)
     * @return this pipeline, for chaining
     * @throws NullPointerException  if {@code angle} is {@code null}
     * @throws IllegalStateException if no source has been opened yet,
     *         or if the pipeline has already been built
     */
    //@ requires angle != null;
    //@ requires !isBuilt();
    //@ requires hasAtLeastOneSource();
    //@ ensures \result == this;
    public EffectPipeline rotate(ParamDouble angle) {
        requireNotBuilt();
        requireHasSource();
        if (angle == null) throw new NullPointerException("angle must not be null");
        currentLayer.transforms.add(new RotateTransform(angle, null, null));
        return this;
    }

    /**
     * Adds a clockwise rotation effect to the current layer, centred on
     * {@code (cx.get(), cy.get())} in stage coordinates.
     *
     * @param angle the rotation angle parameter (in radians, clockwise)
     * @param cx    the x coordinate of the rotation centre
     * @param cy    the y coordinate of the rotation centre
     * @return this pipeline, for chaining
     * @throws NullPointerException  if any parameter is {@code null}
     * @throws IllegalStateException if no source has been opened yet,
     *         or if the pipeline has already been built
     */
    //@ requires angle != null && cx != null && cy != null;
    //@ requires !isBuilt();
    //@ requires hasAtLeastOneSource();
    //@ ensures \result == this;
    public EffectPipeline rotate(ParamDouble angle, ParamDouble cx, ParamDouble cy) {
        requireNotBuilt();
        requireHasSource();
        if (angle == null) throw new NullPointerException("angle must not be null");
        if (cx    == null) throw new NullPointerException("cx must not be null");
        if (cy    == null) throw new NullPointerException("cy must not be null");
        currentLayer.transforms.add(new RotateTransform(angle, cx, cy));
        return this;
    }

    // ─── Build & render ──────────────────────────────────────────────────────────

    /**
     * Finalises the pipeline construction. After this call the
     * pipeline structure is immutable; only {@link #render(Stage)} is
     * permitted (parameters may still vary via the {@link ParamInt}
     * and {@link ParamDouble} instances bound to the effects).
     *
     * @return this pipeline, for chaining
     * @throws IllegalStateException if no source has been added, or if
     *         the pipeline has already been built
     */
    //@ requires !isBuilt();
    //@ requires hasAtLeastOneSource();
    //@ ensures isBuilt();
    //@ ensures \result == this;
    public EffectPipeline build() {
        requireNotBuilt();
        requireHasSource();
        for (Layer layer : layers) {
            layer.transformsArr = layer.transforms.toArray(new Transform[0]);
        }
        built = true;
        return this;
    }

    /**
     * Returns whether {@link #build()} has been called on this pipeline.
     *
     * @return {@code true} if the pipeline has been built
     */
    /*@ pure @*/ public boolean isBuilt() {
        return built;
    }

    /**
     * Returns whether at least one source has been added via
     * {@link #addSource(PixelSource)}.
     *
     * @return {@code true} if the pipeline has at least one source
     */
    /*@ pure @*/ public boolean hasAtLeastOneSource() {
        return hasSource;
    }

    /**
     * Executes the pipeline, writing the resulting frame into the
     * given stage.
     *
     * <p>The stage is first filled with {@link Stage#getBackgroundColor()}.
     * Then, for each pixel {@code (x, y)} of the stage, each layer is
     * applied in order using an <em>always-overwrite</em> rule:</p>
     * <ul>
     *   <li>If the source is bounded ({@link PixelSource#isUnbounded()}
     *       returns {@code false}) and the computed source coordinate falls
     *       outside the source's dimensions, the pixel is not written
     *       (the background color or previous layer remains).</li>
     *   <li>If the source pixel matches the layer's
     *       {@linkplain #transparentColor(int) transparent color}, the
     *       pixel is not written.</li>
     *   <li>Otherwise, the source pixel overwrites the stage pixel.</li>
     * </ul>
     * <p>Fractional source coordinates are resolved by bilinear
     * interpolation using fixed-point arithmetic. For bounded sources the
     * edge pixel is repeated when a bilinear neighbour would fall outside
     * the source.</p>
     *
     * <p>Each layer's pixel rows are rendered in parallel using the
     * common fork-join pool.</p>
     *
     * @param stage the target stage
     * @throws NullPointerException     if {@code stage} is {@code null}
     * @throws IllegalStateException    if the pipeline has not been built
     * @throws IllegalArgumentException if a zoom factor is {@code ≤ 0} at
     *         render time
     */
    //@ requires stage != null;
    //@ requires isBuilt();
    public void render(Stage stage) {
        if (!built) throw new IllegalStateException("Pipeline has not been built");
        if (stage == null) throw new NullPointerException("stage must not be null");

        final int w = stage.getWidth();
        final int h = stage.getHeight();
        final int[] pixels = stage.getPixels();

        Arrays.fill(pixels, stage.getBackgroundColor());

        for (Layer layer : layers) {
            final PixelSource src = layer.source;
            final boolean unbounded = src.isUnbounded();
            final int srcW = src.getWidth();
            final int srcH = src.getHeight();
            final Transform[] transforms = layer.transformsArr;
            final boolean hasTC = layer.hasTransparentColor;
            final int tc = layer.transparentColor;

            for (Transform t : transforms) t.prepare(w, h);

            IntStream.range(0, h).parallel().forEach(y -> {
                final double[] coords = new double[2];
                for (int x = 0; x < w; x++) {
                    coords[0] = x;
                    coords[1] = y;
                    for (Transform t : transforms) t.apply(coords);

                    final double sx = coords[0];
                    final double sy = coords[1];
                    final int x0 = (int) Math.floor(sx);
                    final int y0 = (int) Math.floor(sy);

                    if (!unbounded && (x0 < 0 || x0 >= srcW || y0 < 0 || y0 >= srcH)) continue;

                    final double tx = sx - x0;
                    final double ty = sy - y0;
                    final int pixel;
                    if (tx == 0.0 && ty == 0.0) {
                        pixel = src.getPixel(x0, y0);
                    } else if (ty == 0.0) {
                        final int x1 = unbounded ? x0 + 1 : Math.min(x0 + 1, srcW - 1);
                        pixel = linearH(src.getPixel(x0, y0), src.getPixel(x1, y0), tx);
                    } else if (tx == 0.0) {
                        final int y1 = unbounded ? y0 + 1 : Math.min(y0 + 1, srcH - 1);
                        pixel = linearV(src.getPixel(x0, y0), src.getPixel(x0, y1), ty);
                    } else {
                        final int x1 = unbounded ? x0 + 1 : Math.min(x0 + 1, srcW - 1);
                        final int y1 = unbounded ? y0 + 1 : Math.min(y0 + 1, srcH - 1);
                        pixel = bilinear(
                                src.getPixel(x0, y0), src.getPixel(x1, y0),
                                src.getPixel(x0, y1), src.getPixel(x1, y1),
                                tx, ty);
                    }

                    if (hasTC && pixel == tc) continue;
                    pixels[y * w + x] = pixel;
                }
            });
        }
    }

    // ─── Bilinear sampling helpers ───────────────────────────────────────────────

    /**
     * 1-D horizontal linear interpolation, 8-bit fixed-point.
     * Used when ty == 0.0 exactly (pure H-scroll or horizontal-only fractional position).
     * 2 source samples vs 4 for bilinear: (c0·itx + c1·ftx + 128) >> 8.
     */
    private static int linearH(int p0, int p1, double tx) {
        final int ftx = (int)(tx * 256);
        final int itx = 256 - ftx;
        final int a = ( (p0 >>> 24)        * itx + (p1 >>> 24)        * ftx + 128) >> 8;
        final int r = (((p0 >> 16) & 0xFF) * itx + ((p1 >> 16) & 0xFF) * ftx + 128) >> 8;
        final int g = (((p0 >>  8) & 0xFF) * itx + ((p1 >>  8) & 0xFF) * ftx + 128) >> 8;
        final int b = ( (p0        & 0xFF) * itx + (p1        & 0xFF)  * ftx + 128) >> 8;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 1-D vertical linear interpolation, 8-bit fixed-point.
     * Used when tx == 0.0 exactly (pure V-scroll or vertical-only fractional position).
     * 2 source samples vs 4 for bilinear: (c0·ity + c1·fty + 128) >> 8.
     */
    private static int linearV(int p0, int p1, double ty) {
        final int fty = (int)(ty * 256);
        final int ity = 256 - fty;
        final int a = ( (p0 >>> 24)        * ity + (p1 >>> 24)        * fty + 128) >> 8;
        final int r = (((p0 >> 16) & 0xFF) * ity + ((p1 >> 16) & 0xFF) * fty + 128) >> 8;
        final int g = (((p0 >>  8) & 0xFF) * ity + ((p1 >>  8) & 0xFF) * fty + 128) >> 8;
        final int b = ( (p0        & 0xFF) * ity + (p1        & 0xFF)  * fty + 128) >> 8;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Full 2-D bilinear interpolation using 8-bit fixed-point weights.
     * Weights ftx, fty ∈ [0,255], itx = 256−ftx, ity = 256−fty.
     * Result per channel: (c00·itx·ity + c10·ftx·ity + c01·itx·fty + c11·ftx·fty + 32768) >> 16.
     * Intermediate values fit in int (max ~33M < 2³¹). No clamping required.
     */
    private static int bilinear(int p00, int p10, int p01, int p11, double tx, double ty) {
        final int ftx = (int)(tx * 256);
        final int fty = (int)(ty * 256);
        final int itx = 256 - ftx;
        final int ity = 256 - fty;

        final int a = (((p00 >>> 24)        * itx + (p10 >>> 24)        * ftx) * ity
                     + ((p01 >>> 24)        * itx + (p11 >>> 24)        * ftx) * fty + 32768) >> 16;
        final int r = ((((p00 >> 16) & 0xFF) * itx + ((p10 >> 16) & 0xFF) * ftx) * ity
                     + (((p01 >> 16) & 0xFF) * itx + ((p11 >> 16) & 0xFF) * ftx) * fty + 32768) >> 16;
        final int g = ((((p00 >>  8) & 0xFF) * itx + ((p10 >>  8) & 0xFF) * ftx) * ity
                     + (((p01 >>  8) & 0xFF) * itx + ((p11 >>  8) & 0xFF) * ftx) * fty + 32768) >> 16;
        final int b = (((p00 & 0xFF)         * itx + (p10 & 0xFF)         * ftx) * ity
                     + ((p01 & 0xFF)         * itx + (p11 & 0xFF)         * ftx) * fty + 32768) >> 16;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}