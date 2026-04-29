/*
 * WrappingImageSource.java
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
package fr.dufrenoy.imagefx.source;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * An unbounded {@link PixelSource} backed by a {@link BufferedImage}
 * with toroidal (wrap-around) addressing.
 *
 * <p>Out-of-bounds coordinates are folded back into the image using
 * {@link Math#floorMod}: pixel {@code (x, y)} maps to
 * {@code (floorMod(x, width), floorMod(y, height))}. The source
 * therefore tiles infinitely in all directions, which makes it suitable
 * for horizontally or vertically scrolling layers in a pipeline.</p>
 *
 * <p>Pixel reads are O(1) direct array accesses — no intermediate
 * {@link java.awt.image.Raster} call on the hot path. Immutable
 * after construction.</p>
 */
public final class WrappingImageSource implements PixelSource {

    /*@
      @ public invariant getWidth()  > 0;
      @ public invariant getHeight() > 0;
      @ public invariant isUnbounded();
      @*/

    // ─── Instance variables ──────────────────────────────────────────────────────

    private final int[] pixels;
    private final int   width;
    private final int   height;

    // ─── Constructor ─────────────────────────────────────────────────────────────

    /**
     * Creates a wrapping pixel source backed by the given image.
     *
     * <p>The image is converted to {@code TYPE_INT_ARGB} if necessary so
     * that its backing store is always a {@code DataBufferInt}.</p>
     *
     * @param image the source image
     * @throws NullPointerException if {@code image} is {@code null}
     */
    //@ requires image != null;
    //@ ensures getWidth()  == image.getWidth();
    //@ ensures getHeight() == image.getHeight();
    public WrappingImageSource(BufferedImage image) {
        if (image == null) throw new NullPointerException("image must not be null");
        this.width  = image.getWidth();
        this.height = image.getHeight();
        final BufferedImage argb;
        if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            argb = image;
        } else {
            argb = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            argb.getGraphics().drawImage(image, 0, 0, null);
        }
        this.pixels = ((DataBufferInt) argb.getRaster().getDataBuffer()).getData();
    }

    // ─── PixelSource implementation ──────────────────────────────────────────────

    /**
     * Returns the pixel at the wrapped coordinates
     * {@code (floorMod(x, width), floorMod(y, height))}.
     *
     * <p>Any integer {@code x} and {@code y} are valid; negative values
     * and values beyond the image dimensions are silently wrapped.</p>
     */
    //@ also
    //@ ensures \result == pixels[Math.floorMod(y, height) * width + Math.floorMod(x, width)];
    @Override
    public int getPixel(int x, int y) {
        return pixels[Math.floorMod(y, height) * width + Math.floorMod(x, width)];
    }

    /**
     * {@inheritDoc}
     */
    //@ also
    //@ ensures \result > 0;
    @Override
    /*@ pure @*/ public int getWidth() {
        return width;
    }

    /**
     * {@inheritDoc}
     */
    //@ also
    //@ ensures \result > 0;
    @Override
    /*@ pure @*/ public int getHeight() {
        return height;
    }

    /**
     * Always returns {@code true}: coordinates wrap toroidally.
     */
    //@ also
    //@ ensures \result == true;
    @Override
    /*@ pure @*/ public boolean isUnbounded() {
        return true;
    }
}