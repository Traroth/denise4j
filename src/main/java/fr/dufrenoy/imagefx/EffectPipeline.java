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
package fr.dufrenoy.imagefx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        double srcX(double x, double y);
        double srcY(double x, double y);
    }

    private static final class ScrollHTransform implements Transform {
        final ParamInt offset;
        ScrollHTransform(ParamInt offset) { this.offset = offset; }
        public double srcX(double x, double y) { return x + offset.get(); }
        public double srcY(double x, double y) { return y; }
    }

    private static final class ScrollVTransform implements Transform {
        final ParamInt offset;
        ScrollVTransform(ParamInt offset) { this.offset = offset; }
        public double srcX(double x, double y) { return x; }
        public double srcY(double x, double y) { return y + offset.get(); }
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

        public double srcX(double x, double y) { return cxVal + (x - cxVal) / factorVal; }
        public double srcY(double x, double y) { return cyVal + (y - cyVal) / factorVal; }
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

        public double srcX(double x, double y) {
            double dx = x - cxVal, dy = y - cyVal;
            return cxVal + dx * cosA + dy * sinA;
        }

        public double srcY(double x, double y) {
            double dx = x - cxVal, dy = y - cyVal;
            return cyVal - dx * sinA + dy * cosA;
        }
    }

    // ─── Inner layer representation ──────────────────────────────────────────────

    private static class Layer {
        final PixelSource source;
        int transparentColor;
        boolean hasTransparentColor;
        final List<Transform> transforms = new ArrayList<>();

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
     * interpolation. For bounded sources the edge pixel is repeated when
     * a bilinear neighbour would fall outside the source.</p>
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

        int w = stage.getWidth();
        int h = stage.getHeight();
        int[] pixels = stage.getPixels();

        Arrays.fill(pixels, stage.getBackgroundColor());

        for (Layer layer : layers) {
            PixelSource src = layer.source;
            boolean unbounded = src.isUnbounded();
            int srcW = src.getWidth();
            int srcH = src.getHeight();
            List<Transform> transforms = layer.transforms;

            for (Transform t : transforms) t.prepare(w, h);
            boolean hasTransforms = !transforms.isEmpty();

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    double sx = x, sy = y;
                    if (hasTransforms) {
                        for (Transform t : transforms) {
                            double nsx = t.srcX(sx, sy);
                            sy = t.srcY(sx, sy);
                            sx = nsx;
                        }
                    }

                    int x0 = (int) Math.floor(sx);
                    int y0 = (int) Math.floor(sy);
                    if (!unbounded && (x0 < 0 || x0 >= srcW || y0 < 0 || y0 >= srcH)) continue;

                    double tx = sx - x0;
                    double ty = sy - y0;
                    int pixel;
                    if (tx == 0.0 && ty == 0.0) {
                        pixel = src.getPixel(x0, y0);
                    } else {
                        int x1 = unbounded ? x0 + 1 : Math.min(x0 + 1, srcW - 1);
                        int y1 = unbounded ? y0 + 1 : Math.min(y0 + 1, srcH - 1);
                        pixel = bilinear(
                                src.getPixel(x0, y0), src.getPixel(x1, y0),
                                src.getPixel(x0, y1), src.getPixel(x1, y1),
                                tx, ty);
                    }

                    if (layer.hasTransparentColor && pixel == layer.transparentColor) continue;
                    pixels[y * w + x] = pixel;
                }
            }
        }
    }

    // ─── Bilinear sampling helpers ───────────────────────────────────────────────

    private static int bilinear(int p00, int p10, int p01, int p11, double tx, double ty) {
        double w00 = (1.0 - tx) * (1.0 - ty);
        double w10 = tx          * (1.0 - ty);
        double w01 = (1.0 - tx) * ty;
        double w11 = tx          * ty;
        int a = clamp((int) Math.round((p00 >>> 24) * w00 + (p10 >>> 24) * w10
                                     + (p01 >>> 24) * w01 + (p11 >>> 24) * w11));
        int r = clamp((int) Math.round((p00 >> 16 & 0xFF) * w00 + (p10 >> 16 & 0xFF) * w10
                                     + (p01 >> 16 & 0xFF) * w01 + (p11 >> 16 & 0xFF) * w11));
        int g = clamp((int) Math.round((p00 >>  8 & 0xFF) * w00 + (p10 >>  8 & 0xFF) * w10
                                     + (p01 >>  8 & 0xFF) * w01 + (p11 >>  8 & 0xFF) * w11));
        int b = clamp((int) Math.round((p00        & 0xFF) * w00 + (p10        & 0xFF) * w10
                                     + (p01        & 0xFF) * w01 + (p11        & 0xFF) * w11));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp(int v) { return v < 0 ? 0 : Math.min(v, 255); }
}