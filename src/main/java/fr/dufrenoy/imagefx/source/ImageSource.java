/*
 * ImageSource.java
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
 * A {@link PixelSource} backed by a {@link BufferedImage}.
 *
 * <p>Wraps any {@code BufferedImage} and delegates pixel reads to
 * the underlying image data. Immutable after construction.</p>
 */
public class ImageSource implements PixelSource {

    /*@
      @ public invariant getWidth() > 0;
      @ public invariant getHeight() > 0;
      @*/

    // ─── Instance variables ──────────────────────────────────────────────────────

    private final BufferedImage image;
    private final int[] pixels;
    private final int width;
    private final int height;

    // ─── Constructors ────────────────────────────────────────────────────────────

    /**
     * Creates a new pixel source wrapping the given image.
     *
     * <p>The image is converted to {@code TYPE_INT_ARGB} if necessary so
     * that its backing store is always a {@code DataBufferInt} and pixel
     * reads are O(1) array accesses.</p>
     *
     * @param image the source image
     * @throws NullPointerException if {@code image} is {@code null}
     */
    //@ requires image != null;
    //@ ensures getWidth() == image.getWidth();
    //@ ensures getHeight() == image.getHeight();
    public ImageSource(BufferedImage image) {
        if (image == null) {
            throw new NullPointerException("image must not be null");
        }
        this.width  = image.getWidth();
        this.height = image.getHeight();
        if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            this.image = image;
        } else {
            BufferedImage converted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            converted.getGraphics().drawImage(image, 0, 0, null);
            this.image = converted;
        }
        this.pixels = ((DataBufferInt) this.image.getRaster().getDataBuffer()).getData();
    }

    // ─── PixelSource implementation ──────────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    //@ also
    //@ requires x >= 0 && x < getWidth();
    //@ requires y >= 0 && y < getHeight();
    @Override
    public int getPixel(int x, int y) {
        if (x < 0 || x >= width) {
            throw new IndexOutOfBoundsException(
                    "x " + x + " out of range [0, " + width + ")");
        }
        if (y < 0 || y >= height) {
            throw new IndexOutOfBoundsException(
                    "y " + y + " out of range [0, " + height + ")");
        }
        return pixels[y * width + x];
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
}