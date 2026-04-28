/*
 * Stage.java
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

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * A pixel buffer representing the scene on which effects are rendered.
 *
 * <p>Wraps a {@link BufferedImage} of type {@code TYPE_INT_ARGB} and
 * exposes the underlying {@code int[]} pixel array for direct access.
 * The user retrieves the image via {@link #getImage()} and displays it
 * using the toolkit of their choice (Swing, JavaFX, SWT, file export,
 * etc.).</p>
 *
 * <p>The stage carries a {@linkplain #getBackgroundColor() background
 * color} used by {@link EffectPipeline#render(Stage)} to clear the
 * buffer at the start of each frame. It is therefore visible wherever
 * no layer covers the pixel after compositing. Defaults to opaque
 * black ({@code 0xFF000000}).</p>
 *
 * <p>This class has no dependency on any GUI toolkit.</p>
 */
public class Stage {

    /*@
      @ public invariant getWidth() > 0;
      @ public invariant getHeight() > 0;
      @ public invariant getImage() != null;
      @ public invariant getImage().getType() == java.awt.image.BufferedImage.TYPE_INT_ARGB;
      @ public invariant getPixels() != null;
      @ public invariant getPixels().length == getWidth() * getHeight();
      @*/

    // ─── Instance variables ──────────────────────────────────────────────────────

    private final BufferedImage image;
    private final int[] pixels;
    private final int width;
    private final int height;
    private int backgroundColor;

    // ─── Constructors ────────────────────────────────────────────────────────────

    /**
     * Creates a new stage with the given dimensions and an opaque
     * black background color ({@code 0xFF000000}).
     *
     * @param width  the width in pixels
     * @param height the height in pixels
     * @throws IllegalArgumentException if {@code width} or {@code height}
     *         is not strictly positive
     */
    //@ requires width > 0;
    //@ requires height > 0;
    //@ ensures getWidth() == width;
    //@ ensures getHeight() == height;
    //@ ensures getImage() != null;
    //@ ensures getPixels() != null;
    //@ ensures getPixels().length == width * height;
    //@ ensures getBackgroundColor() == 0xFF000000;
    public Stage(int width, int height) {
        this(width, height, 0xFF000000);
    }

    /**
     * Creates a new stage with the given dimensions and background color.
     *
     * @param width           the width in pixels
     * @param height          the height in pixels
     * @param backgroundColor the background color in ARGB format
     * @throws IllegalArgumentException if {@code width} or {@code height}
     *         is not strictly positive
     */
    //@ requires width > 0;
    //@ requires height > 0;
    //@ ensures getWidth() == width;
    //@ ensures getHeight() == height;
    //@ ensures getImage() != null;
    //@ ensures getPixels() != null;
    //@ ensures getPixels().length == width * height;
    //@ ensures getBackgroundColor() == backgroundColor;
    public Stage(int width, int height, int backgroundColor) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Stage dimensions must be strictly positive: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.backgroundColor = backgroundColor;
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    // ─── Accessors ───────────────────────────────────────────────────────────────

    /**
     * Returns the underlying {@link BufferedImage}.
     *
     * <p>The user can display this image using any toolkit.</p>
     *
     * @return the backing image, never {@code null}
     */
    //@ ensures \result != null;
    /*@ pure @*/ public BufferedImage getImage() {
        return image;
    }

    /**
     * Returns the raw pixel array backing the image.
     *
     * <p>Each element is an ARGB pixel value. Modifications to this
     * array are immediately reflected in the image returned by
     * {@link #getImage()}.</p>
     *
     * @return the pixel array, never {@code null}
     */
    //@ ensures \result != null;
    //@ ensures \result.length == getWidth() * getHeight();
    /*@ pure @*/ public int[] getPixels() {
        return pixels;
    }

    /**
     * Returns the width of this stage in pixels.
     *
     * @return the width, always strictly positive
     */
    //@ ensures \result > 0;
    /*@ pure @*/ public int getWidth() {
        return width;
    }

    /**
     * Returns the height of this stage in pixels.
     *
     * @return the height, always strictly positive
     */
    //@ ensures \result > 0;
    /*@ pure @*/ public int getHeight() {
        return height;
    }

    // ─── Background color ────────────────────────────────────────────────────────

    /**
     * Returns the background color of this stage in ARGB format.
     *
     * <p>This color is used by {@link EffectPipeline#render(Stage)} to
     * clear the buffer at the start of each frame, and is therefore
     * visible wherever no layer covers the pixel after compositing.</p>
     *
     * @return the background color in ARGB format
     */
    /*@ pure @*/ public int getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Sets the background color of this stage.
     *
     * @param argb the new background color in ARGB format
     */
    //@ ensures getBackgroundColor() == argb;
    public void setBackgroundColor(int argb) {
        this.backgroundColor = argb;
    }
}