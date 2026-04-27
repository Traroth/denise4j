/*
 * TileSetWhiteBoxTest.java
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * White-box tests for {@link TileSet}.
 *
 * <p>These tests verify the row-major tile indexing formula:
 * tile {@code i} sits at column {@code i % cols} and row
 * {@code i / cols} of the spritesheet.</p>
 */
class TileSetWhiteBoxTest {

    private static final int RED    = 0xFFFF0000;
    private static final int GREEN  = 0xFF00FF00;
    private static final int BLUE   = 0xFF0000FF;
    private static final int YELLOW = 0xFFFFFF00;
    private static final int CYAN   = 0xFF00FFFF;
    private static final int WHITE  = 0xFFFFFFFF;

    /**
     * Spritesheet: 3 columns × 2 rows of 2×2 tiles → 6 tiles total.
     *
     * <pre>
     *  tile 0 (RED)    tile 1 (GREEN)  tile 2 (BLUE)
     *  tile 3 (YELLOW) tile 4 (CYAN)   tile 5 (WHITE)
     * </pre>
     */
    private TileSet tileSet;

    @BeforeEach
    void setUp() {
        // 6×4 sheet, 2×2 tiles → 3 columns, 2 rows, 6 tiles
        BufferedImage sheet = new BufferedImage(6, 4, BufferedImage.TYPE_INT_ARGB);
        fillTile(sheet, 0, 0, RED);
        fillTile(sheet, 2, 0, GREEN);
        fillTile(sheet, 4, 0, BLUE);
        fillTile(sheet, 0, 2, YELLOW);
        fillTile(sheet, 2, 2, CYAN);
        fillTile(sheet, 4, 2, WHITE);
        tileSet = new TileSet(sheet, 2, 2);
    }

    private static void fillTile(BufferedImage img, int x0, int y0, int argb) {
        for (int dy = 0; dy < 2; dy++)
            for (int dx = 0; dx < 2; dx++)
                img.setRGB(x0 + dx, y0 + dy, argb);
    }

    // ─── Row-major indexing ───────────────────────────────────────────────────────

    @Test
    void tileZeroIsTopLeft() {
        // index 0 → col=0, row=0 → RED
        assertEquals(RED, tileSet.getTilePixel(0, 0, 0));
    }

    @Test
    void tileAtEndOfFirstRowIsCorrect() {
        // index 2 → col=2, row=0 → BLUE
        assertEquals(BLUE, tileSet.getTilePixel(2, 0, 0));
    }

    @Test
    void tileAtStartOfSecondRowIsCorrect() {
        // index 3 → col=0, row=1 → YELLOW (3 / 3 = 1, 3 % 3 = 0)
        assertEquals(YELLOW, tileSet.getTilePixel(3, 0, 0));
    }

    @Test
    void tileAtLastIndexIsBottomRight() {
        // index 5 → col=2, row=1 → WHITE
        assertEquals(WHITE, tileSet.getTilePixel(5, 0, 0));
    }

    // ─── Within-tile pixel coordinates ───────────────────────────────────────────

    @Test
    void tilePixelAtMaxCoordinatesStaysInsideTile() {
        // tile 1 (GREEN) at (tileWidth-1, tileHeight-1) = (1,1) must still be GREEN.
        assertEquals(GREEN, tileSet.getTilePixel(1, 1, 1));
    }

    @Test
    void withinTileCoordinatesDoNotBleedIntoAdjacentTile() {
        // tile 0 at (0,0) = RED; (1,0) must also be RED (still inside tile 0).
        assertEquals(RED, tileSet.getTilePixel(0, 1, 0));
    }
}