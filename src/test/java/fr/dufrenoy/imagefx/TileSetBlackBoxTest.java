/*
 * TileSetBlackBoxTest.java
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Black-box tests for {@link TileSet}.
 *
 * <p>These tests verify the public contract of {@code TileSet} as
 * specified by its Javadoc and JML annotations, with no knowledge
 * of internal implementation.</p>
 */
class TileSetBlackBoxTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    /** Creates a plain ARGB image filled with 0x00000000. */
    private static BufferedImage blankImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Creates a 2×2-tile spritesheet (each tile is tileW×tileH),
     * where each tile is filled with a distinct opaque colour:
     * tile 0 = red, tile 1 = green, tile 2 = blue, tile 3 = white.
     */
    private static BufferedImage fourTileSheet(int tileW, int tileH) {
        int[] colors = { 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF };
        BufferedImage img = new BufferedImage(tileW * 2, tileH * 2, BufferedImage.TYPE_INT_ARGB);
        int[] tileCol = { 0, 1, 0, 1 };
        int[] tileRow = { 0, 0, 1, 1 };
        for (int t = 0; t < 4; t++) {
            for (int y = 0; y < tileH; y++) {
                for (int x = 0; x < tileW; x++) {
                    img.setRGB(tileCol[t] * tileW + x, tileRow[t] * tileH + y, colors[t]);
                }
            }
        }
        return img;
    }

    // ─── Constructor — valid inputs ───────────────────────────────────────────────

    @Test
    void constructorStoresTileDimensions() {
        TileSet ts = new TileSet(blankImage(32, 16), 16, 16);
        assertEquals(16, ts.getTileWidth());
        assertEquals(16, ts.getTileHeight());
    }

    @Test
    void constructorComputesTileCountForSingleTile() {
        TileSet ts = new TileSet(blankImage(16, 16), 16, 16);
        assertEquals(1, ts.getTileCount());
    }

    @Test
    void constructorComputesTileCountForFourTiles() {
        TileSet ts = new TileSet(blankImage(32, 32), 16, 16);
        assertEquals(4, ts.getTileCount());
    }

    @Test
    void constructorComputesTileCountForRectangularSheet() {
        // 4 columns × 2 rows = 8 tiles
        TileSet ts = new TileSet(blankImage(64, 32), 16, 16);
        assertEquals(8, ts.getTileCount());
    }

    @Test
    void constructorAcceptsOnePxByOnePxTiles() {
        TileSet ts = new TileSet(blankImage(3, 2), 1, 1);
        assertEquals(1, ts.getTileWidth());
        assertEquals(1, ts.getTileHeight());
        assertEquals(6, ts.getTileCount());
    }

    @Test
    void constructorAcceptsNonSquareTiles() {
        TileSet ts = new TileSet(blankImage(32, 16), 8, 16);
        assertEquals(8, ts.getTileWidth());
        assertEquals(16, ts.getTileHeight());
        assertEquals(4, ts.getTileCount());
    }

    // ─── Constructor — invalid inputs ─────────────────────────────────────────────

    @Test
    void constructorWithNullImageThrows() {
        assertThrows(NullPointerException.class, () -> new TileSet(null, 16, 16));
    }

    @Test
    void constructorWithZeroTileWidthThrows() {
        assertThrows(IllegalArgumentException.class, () -> new TileSet(blankImage(16, 16), 0, 16));
    }

    @Test
    void constructorWithZeroTileHeightThrows() {
        assertThrows(IllegalArgumentException.class, () -> new TileSet(blankImage(16, 16), 16, 0));
    }

    @Test
    void constructorWithNegativeTileWidthThrows() {
        assertThrows(IllegalArgumentException.class, () -> new TileSet(blankImage(16, 16), -1, 16));
    }

    @Test
    void constructorWithNegativeTileHeightThrows() {
        assertThrows(IllegalArgumentException.class, () -> new TileSet(blankImage(16, 16), 16, -1));
    }

    @Test
    void constructorWithImageWidthNotMultipleOfTileWidthThrows() {
        // 17 is not divisible by 16
        assertThrows(IllegalArgumentException.class, () -> new TileSet(blankImage(17, 16), 16, 16));
    }

    @Test
    void constructorWithImageHeightNotMultipleOfTileHeightThrows() {
        // 17 is not divisible by 16
        assertThrows(IllegalArgumentException.class, () -> new TileSet(blankImage(16, 17), 16, 16));
    }

    // ─── getTilePixel() — reading correct pixels ──────────────────────────────────

    @Test
    void getTilePixelReadsTileZeroTopLeft() {
        TileSet ts = new TileSet(fourTileSheet(8, 8), 8, 8);
        assertEquals(0xFFFF0000, ts.getTilePixel(0, 0, 0));
    }

    @Test
    void getTilePixelReadsTileOneTopLeft() {
        TileSet ts = new TileSet(fourTileSheet(8, 8), 8, 8);
        assertEquals(0xFF00FF00, ts.getTilePixel(1, 0, 0));
    }

    @Test
    void getTilePixelReadsTileTwoTopLeft() {
        TileSet ts = new TileSet(fourTileSheet(8, 8), 8, 8);
        assertEquals(0xFF0000FF, ts.getTilePixel(2, 0, 0));
    }

    @Test
    void getTilePixelReadsTileThreeTopLeft() {
        TileSet ts = new TileSet(fourTileSheet(8, 8), 8, 8);
        assertEquals(0xFFFFFFFF, ts.getTilePixel(3, 0, 0));
    }

    @Test
    void getTilePixelReadsBottomRightPixelOfTile() {
        TileSet ts = new TileSet(fourTileSheet(8, 8), 8, 8);
        // Bottom-right of tile 0 (red) is still red
        assertEquals(0xFFFF0000, ts.getTilePixel(0, 7, 7));
    }

    @Test
    void getTilePixelWorksWithOnePxByOnePxTiles() {
        BufferedImage img = blankImage(2, 1);
        img.setRGB(0, 0, 0xFFABCDEF);
        img.setRGB(1, 0, 0xFF123456);
        TileSet ts = new TileSet(img, 1, 1);
        assertEquals(0xFFABCDEF, ts.getTilePixel(0, 0, 0));
        assertEquals(0xFF123456, ts.getTilePixel(1, 0, 0));
    }

    // ─── getTilePixel() — out-of-bounds ───────────────────────────────────────────

    @Test
    void getTilePixelWithNegativeTileIndexThrows() {
        TileSet ts = new TileSet(blankImage(16, 16), 16, 16);
        assertThrows(IndexOutOfBoundsException.class, () -> ts.getTilePixel(-1, 0, 0));
    }

    @Test
    void getTilePixelWithTileIndexEqualToTileCountThrows() {
        TileSet ts = new TileSet(blankImage(16, 16), 16, 16);
        assertThrows(IndexOutOfBoundsException.class, () -> ts.getTilePixel(1, 0, 0));
    }

    @Test
    void getTilePixelWithXOutOfTileBoundsThrows() {
        TileSet ts = new TileSet(blankImage(16, 16), 16, 16);
        assertThrows(IndexOutOfBoundsException.class, () -> ts.getTilePixel(0, 16, 0));
    }

    @Test
    void getTilePixelWithYOutOfTileBoundsThrows() {
        TileSet ts = new TileSet(blankImage(16, 16), 16, 16);
        assertThrows(IndexOutOfBoundsException.class, () -> ts.getTilePixel(0, 0, 16));
    }

    @Test
    void getTilePixelWithNegativeXThrows() {
        TileSet ts = new TileSet(blankImage(16, 16), 16, 16);
        assertThrows(IndexOutOfBoundsException.class, () -> ts.getTilePixel(0, -1, 0));
    }

    @Test
    void getTilePixelWithNegativeYThrows() {
        TileSet ts = new TileSet(blankImage(16, 16), 16, 16);
        assertThrows(IndexOutOfBoundsException.class, () -> ts.getTilePixel(0, 0, -1));
    }
}