/*
 * PixelSource.java
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

/**
 * A readable source of pixels used by the effect pipeline.
 *
 * <p>Implementations include {@link ImageSource} (wrapping a
 * {@code BufferedImage}) and {@link TileMap} (a tile grid backed
 * by a {@link TileSet}).</p>
 */
public interface PixelSource {

    /*@
      @ public invariant getWidth() > 0;
      @ public invariant getHeight() > 0;
      @*/

    /**
     * Returns the ARGB pixel value at the given coordinates.
     *
     * @param x the horizontal coordinate (0-based, left to right)
     * @param y the vertical coordinate (0-based, top to bottom)
     * @return the pixel value in ARGB format
     * @throws IndexOutOfBoundsException if {@code x} or {@code y} is
     *         outside the source dimensions
     */
    //@ requires !isUnbounded() ==> (x >= 0 && x < getWidth());
    //@ requires !isUnbounded() ==> (y >= 0 && y < getHeight());
    int getPixel(int x, int y);

    /**
     * Returns the width of this pixel source in pixels.
     *
     * @return the width, always strictly positive
     */
    //@ ensures \result > 0;
    /*@ pure @*/ int getWidth();

    /**
     * Returns the height of this pixel source in pixels.
     *
     * @return the height, always strictly positive
     */
    //@ ensures \result > 0;
    /*@ pure @*/ int getHeight();

    /**
     * Returns whether this source accepts pixel coordinates outside its
     * declared bounds.
     *
     * <p>When {@code true}, {@link #getPixel(int, int)} may be called
     * with any integer coordinates and the source handles them
     * internally (e.g. by wrapping). When {@code false}, callers must
     * only call {@link #getPixel(int, int)} with coordinates in
     * {@code [0, getWidth())} × {@code [0, getHeight())}.</p>
     *
     * <p>The default implementation returns {@code false}.</p>
     *
     * @return {@code true} if out-of-bounds coordinates are accepted
     */
    //@ ensures \result == false;
    default /*@ pure @*/ boolean isUnbounded() { return false; }
}