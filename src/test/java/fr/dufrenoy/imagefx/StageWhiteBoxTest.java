/*
 * StageWhiteBoxTest.java
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * White-box tests for {@link Stage}.
 *
 * <p>These tests verify the internal pixel layout: the backing array
 * stores pixel {@code (x, y)} at index {@code y * width + x} (row-major
 * order).</p>
 */
class StageWhiteBoxTest {

    private static final int RED  = 0xFFFF0000;
    private static final int BLUE = 0xFF0000FF;

    @Test
    void pixelStoredAtRowMajorIndex() {
        // Stage 5×3: pixel (x=2, y=1) must be at index 1*5+2 = 7.
        Stage stage = new Stage(5, 3, 0xFF000000);
        stage.getPixels()[7] = RED;
        assertEquals(RED, stage.getImage().getRGB(2, 1));
    }

    @Test
    void pixelAtOriginIsIndexZero() {
        Stage stage = new Stage(4, 4, 0xFF000000);
        stage.getPixels()[0] = BLUE;
        assertEquals(BLUE, stage.getImage().getRGB(0, 0));
    }

    @Test
    void pixelAtLastPositionIsLastIndex() {
        // Stage 3×2: pixel (2,1) is at index 1*3+2 = 5 (last index).
        Stage stage = new Stage(3, 2, 0xFF000000);
        stage.getPixels()[5] = RED;
        assertEquals(RED, stage.getImage().getRGB(2, 1));
    }

    @Test
    void pixelArrayLengthIsWidthTimesHeight() {
        Stage stage = new Stage(7, 11, 0);
        assertEquals(7 * 11, stage.getPixels().length);
    }

    @Test
    void adjacentColumnsAreAdjacentInArray() {
        // Pixels (0,0) and (1,0) are at index 0 and 1 — consecutive in the array.
        Stage stage = new Stage(4, 2, 0xFF000000);
        int[] pixels = stage.getPixels();
        pixels[0] = RED;
        pixels[1] = BLUE;
        assertEquals(RED,  stage.getImage().getRGB(0, 0));
        assertEquals(BLUE, stage.getImage().getRGB(1, 0));
    }

    @Test
    void adjacentRowsAreWidthApartInArray() {
        // Pixels (0,0) at index 0 and (0,1) at index width = 4.
        Stage stage = new Stage(4, 2, 0xFF000000);
        int[] pixels = stage.getPixels();
        pixels[0] = RED;
        pixels[4] = BLUE; // same column, next row
        assertEquals(RED,  stage.getImage().getRGB(0, 0));
        assertEquals(BLUE, stage.getImage().getRGB(0, 1));
    }
}