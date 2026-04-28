/*
 * TileMap.java
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

/**
 * A grid of tiles referencing a {@link TileSet}. Implements
 * {@link PixelSource} so it can be used as a source in the
 * {@link EffectPipeline}.
 *
 * <p>The map is a 2D grid of tile indices. Each index refers to a
 * tile in the associated {@code TileSet}. The map's pixel dimensions
 * are {@code cols * tileWidth} by {@code rows * tileHeight}.</p>
 *
 * <p>The developer controls tile data via {@link #setTiles(int, int, int[][])},
 * which writes a rectangular region of tile indices into the grid.</p>
 *
 * <p>The {@link EdgePolicy} determines what happens when pixel
 * coordinates fall outside the grid boundaries.</p>
 */
public class TileMap implements PixelSource {

    /*@
      @ public invariant getTileSet() != null;
      @ public invariant getCols() > 0;
      @ public invariant getRows() > 0;
      @ public invariant getEdgePolicy() != null;
      @ public invariant getWidth() == getCols() * getTileSet().getTileWidth();
      @ public invariant getHeight() == getRows() * getTileSet().getTileHeight();
      @*/

    // ─── Edge policy ─────────────────────────────────────────────────────────────

    /**
     * Determines the behaviour when pixel coordinates fall outside the
     * grid boundaries.
     */
    public enum EdgePolicy {

        /** The map wraps around (coordinates are taken modulo the map size). */
        WRAP,

        /**
         * Out-of-bounds pixel coordinates throw
         * {@link IndexOutOfBoundsException}. The {@link EffectPipeline}
         * bounds-checks non-unbounded sources before calling
         * {@link TileMap#getPixel(int, int)}, so out-of-bounds positions
         * simply leave the stage's background color visible.
         */
        CLIP,

        /**
         * The developer feeds new tiles progressively as the view scrolls.
         * Out-of-bounds pixel coordinates throw
         * {@link IndexOutOfBoundsException}, identical to {@link #CLIP}
         * at runtime. The distinction is purely semantic: {@code FEED}
         * signals that the map will be updated dynamically via
         * {@link TileMap#setTiles(int, int, int[][])}.
         */
        FEED
    }

    // ─── Instance variables ──────────────────────────────────────────────────────

    private final TileSet tileSet;
    private final int cols;
    private final int rows;
    private final EdgePolicy edgePolicy;
    private final int[][] grid;

    // ─── Constructors ────────────────────────────────────────────────────────────

    /**
     * Creates a new tile map with the given dimensions and edge policy.
     *
     * <p>The grid is initially filled with tile index 0.</p>
     *
     * @param tileSet    the tile set to reference
     * @param cols       the number of tile columns
     * @param rows       the number of tile rows
     * @param edgePolicy the edge policy
     * @throws NullPointerException     if {@code tileSet} or
     *         {@code edgePolicy} is {@code null}
     * @throws IllegalArgumentException if {@code cols} or {@code rows}
     *         is not strictly positive
     */
    //@ requires tileSet != null;
    //@ requires cols > 0;
    //@ requires rows > 0;
    //@ requires edgePolicy != null;
    //@ ensures getCols() == cols;
    //@ ensures getRows() == rows;
    //@ ensures getEdgePolicy() == edgePolicy;
    public TileMap(TileSet tileSet, int cols, int rows, EdgePolicy edgePolicy) {
        if (tileSet == null) {
            throw new NullPointerException("tileSet must not be null");
        }
        if (edgePolicy == null) {
            throw new NullPointerException("edgePolicy must not be null");
        }
        if (cols <= 0 || rows <= 0) {
            throw new IllegalArgumentException(
                    "Map dimensions must be strictly positive: " + cols + "x" + rows);
        }
        this.tileSet = tileSet;
        this.cols = cols;
        this.rows = rows;
        this.edgePolicy = edgePolicy;
        this.grid = new int[rows][cols];
    }

    // ─── Tile data ───────────────────────────────────────────────────────────────

    /**
     * Writes a rectangular region of tile indices into the grid.
     *
     * <p>The {@code tileIds} array is indexed as
     * {@code tileIds[row][col]}. The region starts at grid position
     * ({@code startCol}, {@code startRow}).</p>
     *
     * @param startCol the starting column in the grid
     * @param startRow the starting row in the grid
     * @param tileIds  the rectangular block of tile indices to write
     * @throws NullPointerException      if {@code tileIds} is {@code null}
     * @throws IndexOutOfBoundsException if the region extends beyond
     *         the grid boundaries
     * @throws IllegalArgumentException  if any tile index is negative or
     *         greater than or equal to the tile set's tile count
     */
    //@ requires tileIds != null;
    //@ requires startCol >= 0;
    //@ requires startRow >= 0;
    public void setTiles(int startCol, int startRow, int[][] tileIds) {
        if (tileIds == null) {
            throw new NullPointerException("tileIds must not be null");
        }
        int regionRows = tileIds.length;
        int regionCols = regionRows > 0 ? tileIds[0].length : 0;
        if (startRow + regionRows > rows || startCol + regionCols > cols) {
            throw new IndexOutOfBoundsException(
                    "Region [" + startCol + ".." + (startCol + regionCols - 1)
                    + ", " + startRow + ".." + (startRow + regionRows - 1)
                    + "] exceeds grid bounds [0.." + (cols - 1) + ", 0.." + (rows - 1) + "]");
        }
        int tileCount = tileSet.getTileCount();
        for (int r = 0; r < regionRows; r++) {
            for (int c = 0; c < regionCols; c++) {
                int index = tileIds[r][c];
                if (index < 0 || index >= tileCount) {
                    throw new IllegalArgumentException(
                            "Tile index " + index + " out of range [0, " + tileCount + ")");
                }
                grid[startRow + r][startCol + c] = index;
            }
        }
    }

    /**
     * Returns the tile index at the given grid position.
     *
     * @param col the column in the grid
     * @param row the row in the grid
     * @return the tile index
     * @throws IndexOutOfBoundsException if {@code col} or {@code row}
     *         is outside the grid
     */
    //@ requires col >= 0 && col < getCols();
    //@ requires row >= 0 && row < getRows();
    //@ ensures \result >= 0;
    /*@ pure @*/ public int getTileIndex(int col, int row) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException(
                    "Grid position (" + col + ", " + row + ") out of bounds ["
                    + cols + "x" + rows + "]");
        }
        return grid[row][col];
    }

    // ─── Accessors ───────────────────────────────────────────────────────────────

    /**
     * Returns the tile set referenced by this map.
     *
     * @return the tile set, never {@code null}
     */
    //@ ensures \result != null;
    /*@ pure @*/ public TileSet getTileSet() {
        return tileSet;
    }

    /**
     * Returns the number of tile columns.
     *
     * @return the column count, always strictly positive
     */
    //@ ensures \result > 0;
    /*@ pure @*/ public int getCols() {
        return cols;
    }

    /**
     * Returns the number of tile rows.
     *
     * @return the row count, always strictly positive
     */
    //@ ensures \result > 0;
    /*@ pure @*/ public int getRows() {
        return rows;
    }

    /**
     * Returns the edge policy.
     *
     * @return the edge policy, never {@code null}
     */
    //@ ensures \result != null;
    /*@ pure @*/ public EdgePolicy getEdgePolicy() {
        return edgePolicy;
    }

    // ─── PixelSource implementation ──────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>For {@link EdgePolicy#WRAP}, any integer coordinates are
     * accepted and wrapped modulo the map dimensions. For
     * {@link EdgePolicy#CLIP} and {@link EdgePolicy#FEED}, coordinates
     * must be within {@code [0, getWidth())} × {@code [0, getHeight())};
     * out-of-bounds coordinates throw {@link IndexOutOfBoundsException}.
     * The {@link fr.dufrenoy.imagefx.EffectPipeline pipeline}
     * bounds-checks non-unbounded sources before calling this method,
     * so out-of-bounds pixels simply leave the stage's background
     * color visible.</p>
     */
    //@ also
    //@ requires edgePolicy == EdgePolicy.WRAP || (x >= 0 && x < getWidth() && y >= 0 && y < getHeight());
    //@ ensures (* \result is the ARGB pixel of the tile covering (x, y), after optional wrapping *);
    @Override
    public int getPixel(int x, int y) {
        int w = getWidth();
        int h = getHeight();
        if (edgePolicy == EdgePolicy.WRAP) {
            x = Math.floorMod(x, w);
            y = Math.floorMod(y, h);
        } else if (x < 0 || x >= w || y < 0 || y >= h) {
            throw new IndexOutOfBoundsException(
                    "Pixel (" + x + ", " + y + ") out of bounds [" + w + "x" + h + "]");
        }
        int tileCol = x / tileSet.getTileWidth();
        int tileRow = y / tileSet.getTileHeight();
        int px = x % tileSet.getTileWidth();
        int py = y % tileSet.getTileHeight();
        return tileSet.getTilePixel(grid[tileRow][tileCol], px, py);
    }

    /**
     * Returns {@code true} if the edge policy is {@link EdgePolicy#WRAP},
     * meaning any integer coordinates are accepted by
     * {@link #getPixel(int, int)}.
     *
     * @return {@code true} for WRAP, {@code false} for CLIP and FEED
     */
    //@ also
    //@ ensures \result == (getEdgePolicy() == EdgePolicy.WRAP);
    @Override
    public /*@ pure @*/ boolean isUnbounded() {
        return edgePolicy == EdgePolicy.WRAP;
    }

    /**
     * {@inheritDoc}
     */
    //@ also
    //@ ensures \result == getCols() * getTileSet().getTileWidth();
    @Override
    /*@ pure @*/ public int getWidth() {
        return cols * tileSet.getTileWidth();
    }

    /**
     * {@inheritDoc}
     */
    //@ also
    //@ ensures \result == getRows() * getTileSet().getTileHeight();
    @Override
    /*@ pure @*/ public int getHeight() {
        return rows * tileSet.getTileHeight();
    }
}