/*
 * TileMapWhiteBoxTest.java
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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * White-box tests for {@link TileMap}.
 *
 * <p>Tests focus on:</p>
 * <ul>
 *   <li>WRAP wrapping via {@code Math.floorMod} — including large negative
 *       coordinates and multiples of the map dimension.</li>
 *   <li>FEED and CLIP having identical runtime behaviour.</li>
 * </ul>
 */
class TileMapWhiteBoxTest {

    private static final int RED   = 0xFFFF0000;
    private static final int GREEN = 0xFF00FF00;
    private static final int BLUE  = 0xFF0000FF;

    /** 3×1 WRAP TileMap: col 0 = RED tile, col 1 = GREEN tile, col 2 = BLUE tile. */
    private TileMap wrapMap;

    /** Same layout as wrapMap but CLIP. */
    private TileMap clipMap;

    /** Same layout as wrapMap but FEED. */
    private TileMap feedMap;

    @BeforeEach
    void setUp() {
        BufferedImage sheet = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        sheet.setRGB(0, 0, RED);
        sheet.setRGB(1, 0, GREEN);
        sheet.setRGB(2, 0, BLUE);
        TileSet ts = new TileSet(sheet, 1, 1);

        wrapMap = new TileMap(ts, 3, 1, TileMap.EdgePolicy.WRAP);
        clipMap = new TileMap(ts, 3, 1, TileMap.EdgePolicy.CLIP);
        feedMap = new TileMap(ts, 3, 1, TileMap.EdgePolicy.FEED);

        int[][] tiles = {{ 0, 1, 2 }};
        wrapMap.setTiles(0, 0, tiles);
        clipMap.setTiles(0, 0, tiles);
        feedMap.setTiles(0, 0, tiles);
    }

    // ─── WRAP — floorMod correctness ─────────────────────────────────────────────

    @Test
    void wrapAtExactMapWidth() {
        // x = width (3) wraps to 0 → RED
        assertEquals(RED, wrapMap.getPixel(3, 0));
    }

    @Test
    void wrapAtNegativeOne() {
        // x = -1 → floorMod(-1, 3) = 2 → BLUE
        assertEquals(BLUE, wrapMap.getPixel(-1, 0));
    }

    @Test
    void wrapAtLargeNegativeCoordinate() {
        // x = -3*width + 1 = -8 → floorMod(-8, 3) = 1 → GREEN
        assertEquals(GREEN, wrapMap.getPixel(-3 * 3 + 1, 0));
    }

    @Test
    void wrapAtMultipleOfWidth() {
        // x = 6 = 2*width → floorMod(6, 3) = 0 → RED
        assertEquals(RED, wrapMap.getPixel(6, 0));
    }

    @Test
    void wrapAtNegativeMultipleOfWidth() {
        // x = -6 = -2*width → floorMod(-6, 3) = 0 → RED
        assertEquals(RED, wrapMap.getPixel(-6, 0));
    }

    // ─── FEED = CLIP at runtime ───────────────────────────────────────────────────

    @Test
    void feedAndClipReturnSamePixelInsideBounds() {
        assertEquals(clipMap.getPixel(0, 0), feedMap.getPixel(0, 0));
        assertEquals(clipMap.getPixel(1, 0), feedMap.getPixel(1, 0));
        assertEquals(clipMap.getPixel(2, 0), feedMap.getPixel(2, 0));
    }

    @Test
    void feedThrowsOutOfBoundsLikeClip() {
        assertThrows(IndexOutOfBoundsException.class, () -> feedMap.getPixel(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> clipMap.getPixel(-1, 0));
    }

    @Test
    void feedThrowsAtWidthLikeClip() {
        assertThrows(IndexOutOfBoundsException.class, () -> feedMap.getPixel(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> clipMap.getPixel(3, 0));
    }
}