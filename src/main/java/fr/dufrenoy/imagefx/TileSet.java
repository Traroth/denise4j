/*
 * TileSet.java
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

import java.awt.image.BufferedImage;

/**
 * A spritesheet divided into fixed-size tiles.
 *
 * <p>Tiles are indexed row by row, from top-left to bottom-right
 * (reading order). Index 0 is the top-left tile.</p>
 *
 * <p>Immutable after construction.</p>
 */
public class TileSet {

    /*@
      @ public invariant getTileWidth() > 0;
      @ public invariant getTileHeight() > 0;
      @ public invariant getTileCount() > 0;
      @*/

    // ─── Instance variables ──────────────────────────────────────────────────────

    private final BufferedImage image;
    private final int tileWidth;
    private final int tileHeight;
    private final int columns;
    private final int rows;
    private final int tileCount;

    // ─── Constructors ────────────────────────────────────────────────────────────

    /**
     * Creates a new tile set from the given spritesheet.
     *
     * <p>The image dimensions must be exact multiples of the tile
     * dimensions.</p>
     *
     * @param image      the spritesheet image
     * @param tileWidth  the width of each tile in pixels
     * @param tileHeight the height of each tile in pixels
     * @throws NullPointerException     if {@code image} is {@code null}
     * @throws IllegalArgumentException if {@code tileWidth} or
     *         {@code tileHeight} is not strictly positive, or if the
     *         image dimensions are not exact multiples of the tile dimensions
     */
    //@ requires image != null;
    //@ requires tileWidth > 0;
    //@ requires tileHeight > 0;
    //@ ensures getTileWidth() == tileWidth;
    //@ ensures getTileHeight() == tileHeight;
    //@ ensures getTileCount() == (image.getWidth() / tileWidth) * (image.getHeight() / tileHeight);
    public TileSet(BufferedImage image, int tileWidth, int tileHeight) {
        if (image == null) {
            throw new NullPointerException("image must not be null");
        }
        if (tileWidth <= 0 || tileHeight <= 0) {
            throw new IllegalArgumentException(
                    "Tile dimensions must be strictly positive: " + tileWidth + "x" + tileHeight);
        }
        if (image.getWidth() % tileWidth != 0 || image.getHeight() % tileHeight != 0) {
            throw new IllegalArgumentException(
                    "Image dimensions (" + image.getWidth() + "x" + image.getHeight()
                    + ") must be exact multiples of tile dimensions ("
                    + tileWidth + "x" + tileHeight + ")");
        }
        this.image = image;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.columns = image.getWidth() / tileWidth;
        this.rows = image.getHeight() / tileHeight;
        this.tileCount = this.columns * this.rows;
    }

    // ─── Accessors ───────────────────────────────────────────────────────────────

    /**
     * Returns the width of each tile in pixels.
     *
     * @return the tile width, always strictly positive
     */
    //@ ensures \result > 0;
    /*@ pure @*/ public int getTileWidth() {
        return tileWidth;
    }

    /**
     * Returns the height of each tile in pixels.
     *
     * @return the tile height, always strictly positive
     */
    //@ ensures \result > 0;
    /*@ pure @*/ public int getTileHeight() {
        return tileHeight;
    }

    /**
     * Returns the total number of tiles in this set.
     *
     * @return the tile count, always strictly positive
     */
    //@ ensures \result > 0;
    /*@ pure @*/ public int getTileCount() {
        return tileCount;
    }

    /**
     * Returns the ARGB pixel value at the given coordinates within
     * the specified tile.
     *
     * @param tileIndex the tile index (0-based, reading order)
     * @param x         the horizontal coordinate within the tile
     * @param y         the vertical coordinate within the tile
     * @return the pixel value in ARGB format
     * @throws IndexOutOfBoundsException if {@code tileIndex} is negative
     *         or greater than or equal to {@link #getTileCount()}, or if
     *         {@code x} or {@code y} is outside the tile dimensions
     */
    //@ requires tileIndex >= 0 && tileIndex < getTileCount();
    //@ requires x >= 0 && x < getTileWidth();
    //@ requires y >= 0 && y < getTileHeight();
    //@ ensures (* \result is a valid ARGB value *);
    public int getTilePixel(int tileIndex, int x, int y) {
        if (tileIndex < 0 || tileIndex >= tileCount) {
            throw new IndexOutOfBoundsException(
                    "tileIndex " + tileIndex + " out of range [0, " + tileCount + ")");
        }
        if (x < 0 || x >= tileWidth) {
            throw new IndexOutOfBoundsException(
                    "x " + x + " out of range [0, " + tileWidth + ")");
        }
        if (y < 0 || y >= tileHeight) {
            throw new IndexOutOfBoundsException(
                    "y " + y + " out of range [0, " + tileHeight + ")");
        }
        int col = tileIndex % columns;
        int row = tileIndex / columns;
        return image.getRGB(col * tileWidth + x, row * tileHeight + y);
    }
}