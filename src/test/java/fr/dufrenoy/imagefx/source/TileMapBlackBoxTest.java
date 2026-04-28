/*
 * TileMapBlackBoxTest.java
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Black-box tests for {@link TileMap}.
 *
 * <p>These tests verify the public contract of {@code TileMap} as
 * specified by its Javadoc and JML annotations, with no knowledge
 * of internal implementation.</p>
 *
 * <p>Fixture: a {@link TileSet} of four 1×1-pixel tiles, one per
 * colour (red, green, blue, white), stored in a 4×1 spritesheet.
 * All maps are 3 columns × 2 rows unless otherwise noted, giving a
 * pixel size of 3×2.</p>
 */
class TileMapBlackBoxTest {

    private static final int RED   = 0xFFFF0000;
    private static final int GREEN = 0xFF00FF00;
    private static final int BLUE  = 0xFF0000FF;
    private static final int WHITE = 0xFFFFFFFF;

    /** 4 tiles (1×1 px each): tile 0=red, 1=green, 2=blue, 3=white. */
    private TileSet ts;

    @BeforeEach
    void setUp() {
        BufferedImage sheet = new BufferedImage(4, 1, BufferedImage.TYPE_INT_ARGB);
        sheet.setRGB(0, 0, RED);
        sheet.setRGB(1, 0, GREEN);
        sheet.setRGB(2, 0, BLUE);
        sheet.setRGB(3, 0, WHITE);
        ts = new TileSet(sheet, 1, 1);
    }

    // ─── Constructor — valid inputs ───────────────────────────────────────────────

    @Test
    void constructorStoresDimensions() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        assertEquals(3, map.getCols());
        assertEquals(2, map.getRows());
    }

    @Test
    void constructorStoresEdgePolicy() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.CLIP);
        assertEquals(TileMap.EdgePolicy.CLIP, map.getEdgePolicy());
    }

    @Test
    void constructorStoresTileSet() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        assertSame(ts, map.getTileSet());
    }

    @Test
    void constructorInitializesGridToTileZero() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                assertEquals(0, map.getTileIndex(c, r),
                        "Expected tile index 0 at (" + c + ", " + r + ")");
            }
        }
    }

    @Test
    void getWidthEqualColsTimesTileWidth() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        assertEquals(3, map.getWidth()); // 3 cols × 1px tile
    }

    @Test
    void getHeightEqualRowsTimesTileHeight() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        assertEquals(2, map.getHeight()); // 2 rows × 1px tile
    }

    @Test
    void constructorWithOneBySizeOneIsAccepted() {
        TileMap map = new TileMap(ts, 1, 1, TileMap.EdgePolicy.CLIP);
        assertEquals(1, map.getCols());
        assertEquals(1, map.getRows());
    }

    // ─── Constructor — invalid inputs ─────────────────────────────────────────────

    @Test
    void constructorWithNullTileSetThrows() {
        assertThrows(NullPointerException.class,
                () -> new TileMap(null, 3, 2, TileMap.EdgePolicy.WRAP));
    }

    @Test
    void constructorWithNullEdgePolicyThrows() {
        assertThrows(NullPointerException.class,
                () -> new TileMap(ts, 3, 2, null));
    }

    @Test
    void constructorWithZeroColsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new TileMap(ts, 0, 2, TileMap.EdgePolicy.WRAP));
    }

    @Test
    void constructorWithZeroRowsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new TileMap(ts, 3, 0, TileMap.EdgePolicy.WRAP));
    }

    @Test
    void constructorWithNegativeColsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new TileMap(ts, -1, 2, TileMap.EdgePolicy.WRAP));
    }

    @Test
    void constructorWithNegativeRowsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new TileMap(ts, 3, -1, TileMap.EdgePolicy.WRAP));
    }

    // ─── setTiles() ───────────────────────────────────────────────────────────────

    @Test
    void setTilesSingleTile() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        map.setTiles(1, 0, new int[][] { { 2 } });
        assertEquals(2, map.getTileIndex(1, 0));
    }

    @Test
    void setTilesFullGrid() {
        TileMap map = new TileMap(ts, 2, 2, TileMap.EdgePolicy.WRAP);
        map.setTiles(0, 0, new int[][] { { 0, 1 }, { 2, 3 } });
        assertEquals(0, map.getTileIndex(0, 0));
        assertEquals(1, map.getTileIndex(1, 0));
        assertEquals(2, map.getTileIndex(0, 1));
        assertEquals(3, map.getTileIndex(1, 1));
    }

    @Test
    void setTilesPartialRegion() {
        TileMap map = new TileMap(ts, 3, 3, TileMap.EdgePolicy.WRAP);
        map.setTiles(1, 1, new int[][] { { 1, 2 } });
        assertEquals(1, map.getTileIndex(1, 1));
        assertEquals(2, map.getTileIndex(2, 1));
        // Adjacent cells untouched
        assertEquals(0, map.getTileIndex(0, 1));
        assertEquals(0, map.getTileIndex(0, 0));
    }

    @Test
    void setTilesWithNullThrows() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        assertThrows(NullPointerException.class, () -> map.setTiles(0, 0, null));
    }

    @Test
    void setTilesRegionExceedingColsThrows() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        // startCol=2 + 2 cols wide = 4 > 3
        assertThrows(IndexOutOfBoundsException.class,
                () -> map.setTiles(2, 0, new int[][] { { 0, 1 } }));
    }

    @Test
    void setTilesRegionExceedingRowsThrows() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        // startRow=1 + 2 rows tall = 3 > 2
        assertThrows(IndexOutOfBoundsException.class,
                () -> map.setTiles(0, 1, new int[][] { { 0 }, { 1 } }));
    }

    @Test
    void setTilesWithNegativeTileIndexThrows() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        assertThrows(IllegalArgumentException.class,
                () -> map.setTiles(0, 0, new int[][] { { -1 } }));
    }

    @Test
    void setTilesWithTileIndexEqualToTileCountThrows() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        // ts has 4 tiles, index 4 is out of range
        assertThrows(IllegalArgumentException.class,
                () -> map.setTiles(0, 0, new int[][] { { 4 } }));
    }

    // ─── getTileIndex() ───────────────────────────────────────────────────────────

    @Test
    void getTileIndexReflectsSetTiles() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        map.setTiles(0, 0, new int[][] { { 3, 1, 2 }, { 0, 2, 1 } });
        assertEquals(3, map.getTileIndex(0, 0));
        assertEquals(1, map.getTileIndex(1, 0));
        assertEquals(2, map.getTileIndex(2, 0));
        assertEquals(0, map.getTileIndex(0, 1));
        assertEquals(2, map.getTileIndex(1, 1));
        assertEquals(1, map.getTileIndex(2, 1));
    }

    @Test
    void getTileIndexOutOfBoundsThrows() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        assertThrows(IndexOutOfBoundsException.class, () -> map.getTileIndex(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> map.getTileIndex(0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> map.getTileIndex(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> map.getTileIndex(0, -1));
    }

    // ─── getPixel() — WRAP ────────────────────────────────────────────────────────

    @Test
    void getPixelWrapInBoundsReturnsCorrectColor() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        map.setTiles(0, 0, new int[][] { { 0, 1, 2 }, { 3, 0, 1 } });
        assertEquals(RED,   map.getPixel(0, 0)); // tile 0
        assertEquals(GREEN, map.getPixel(1, 0)); // tile 1
        assertEquals(BLUE,  map.getPixel(2, 0)); // tile 2
        assertEquals(WHITE, map.getPixel(0, 1)); // tile 3
    }

    @Test
    void getPixelWrapXEqualToWidthWrapsToColumn0() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        map.setTiles(0, 0, new int[][] { { 1, 0, 0 }, { 0, 0, 0 } });
        assertEquals(map.getPixel(0, 0), map.getPixel(3, 0)); // 3 == width
    }

    @Test
    void getPixelWrapXTwiceWidthWrapsAgain() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        map.setTiles(0, 0, new int[][] { { 1, 0, 0 }, { 0, 0, 0 } });
        assertEquals(map.getPixel(0, 0), map.getPixel(6, 0)); // 6 == 2 * width
    }

    @Test
    void getPixelWrapYEqualToHeightWrapsToRow0() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        map.setTiles(0, 0, new int[][] { { 1, 0, 0 }, { 0, 0, 0 } });
        assertEquals(map.getPixel(0, 0), map.getPixel(0, 2)); // 2 == height
    }

    @Test
    void getPixelWrapNegativeXWrapsFromRight() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        map.setTiles(0, 0, new int[][] { { 0, 0, 2 }, { 0, 0, 0 } });
        // x=-1 should wrap to x=2 (rightmost column)
        assertEquals(map.getPixel(2, 0), map.getPixel(-1, 0));
    }

    @Test
    void getPixelWrapNegativeYWrapsFromBottom() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP);
        map.setTiles(0, 0, new int[][] { { 0, 0, 0 }, { 3, 0, 0 } });
        // y=-1 should wrap to y=1 (bottom row)
        assertEquals(map.getPixel(0, 1), map.getPixel(0, -1));
    }

    // ─── getPixel() — CLIP ────────────────────────────────────────────────────────

    @Test
    void getPixelClipInBoundsReturnsCorrectColor() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.CLIP);
        map.setTiles(0, 0, new int[][] { { 1, 0, 0 }, { 0, 0, 0 } });
        assertEquals(GREEN, map.getPixel(0, 0));
    }

    @Test
    void getPixelClipNegativeXThrows() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.CLIP);
        assertThrows(IndexOutOfBoundsException.class, () -> map.getPixel(-1, 0));
    }

    @Test
    void getPixelClipXEqualToWidthThrows() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.CLIP);
        assertThrows(IndexOutOfBoundsException.class, () -> map.getPixel(3, 0));
    }

    @Test
    void getPixelClipNegativeYThrows() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.CLIP);
        assertThrows(IndexOutOfBoundsException.class, () -> map.getPixel(0, -1));
    }

    @Test
    void getPixelClipYEqualToHeightThrows() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.CLIP);
        assertThrows(IndexOutOfBoundsException.class, () -> map.getPixel(0, 2));
    }

    // ─── getPixel() — FEED ────────────────────────────────────────────────────────

    @Test
    void getPixelFeedInBoundsReturnsCorrectColor() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.FEED);
        map.setTiles(0, 0, new int[][] { { 2, 0, 0 }, { 0, 0, 0 } });
        assertEquals(BLUE, map.getPixel(0, 0));
    }

    @Test
    void getPixelFeedNegativeXThrows() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.FEED);
        assertThrows(IndexOutOfBoundsException.class, () -> map.getPixel(-1, 0));
    }

    @Test
    void getPixelFeedXEqualToWidthThrows() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.FEED);
        assertThrows(IndexOutOfBoundsException.class, () -> map.getPixel(3, 0));
    }

    @Test
    void getPixelFeedNegativeYThrows() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.FEED);
        assertThrows(IndexOutOfBoundsException.class, () -> map.getPixel(0, -1));
    }

    @Test
    void getPixelFeedYEqualToHeightThrows() {
        TileMap map = new TileMap(ts, 3, 2, TileMap.EdgePolicy.FEED);
        assertThrows(IndexOutOfBoundsException.class, () -> map.getPixel(0, 2));
    }

    // ─── isUnbounded() ───────────────────────────────────────────────────────────

    @Test
    void isUnboundedTrueForWrap() {
        assertTrue(new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP).isUnbounded());
    }

    @Test
    void isUnboundedFalseForClip() {
        assertFalse(new TileMap(ts, 3, 2, TileMap.EdgePolicy.CLIP).isUnbounded());
    }

    @Test
    void isUnboundedFalseForFeed() {
        assertFalse(new TileMap(ts, 3, 2, TileMap.EdgePolicy.FEED).isUnbounded());
    }

    // ─── getTileSet() ─────────────────────────────────────────────────────────────

    @Test
    void getTileSetReturnsNonNull() {
        assertNotNull(new TileMap(ts, 3, 2, TileMap.EdgePolicy.WRAP).getTileSet());
    }
}