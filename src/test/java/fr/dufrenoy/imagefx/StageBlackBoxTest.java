/*
 * StageBlackBoxTest.java
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Black-box tests for {@link Stage}.
 *
 * <p>These tests verify the public contract of {@code Stage} as
 * specified by its Javadoc and JML annotations, with no knowledge
 * of internal implementation.</p>
 */
class StageBlackBoxTest {

    // ─── Constructor(width, height) ──────────────────────────────────────────────

    @Test
    void constructorStoresDimensions() {
        Stage stage = new Stage(320, 200);
        assertEquals(320, stage.getWidth());
        assertEquals(200, stage.getHeight());
    }

    @Test
    void constructorWithOneBySizeOneIsAccepted() {
        Stage stage = new Stage(1, 1);
        assertEquals(1, stage.getWidth());
        assertEquals(1, stage.getHeight());
    }

    @Test
    void constructorWithNonSquareDimensionsIsAccepted() {
        Stage stage = new Stage(800, 1);
        assertEquals(800, stage.getWidth());
        assertEquals(1, stage.getHeight());
    }

    @Test
    void constructorSetsDefaultBackgroundColorToOpaqueBlack() {
        Stage stage = new Stage(10, 10);
        assertEquals(0xFF000000, stage.getBackgroundColor());
    }

    @Test
    void constructorWithZeroWidthThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Stage(0, 100));
    }

    @Test
    void constructorWithZeroHeightThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Stage(100, 0));
    }

    @Test
    void constructorWithNegativeWidthThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Stage(-1, 100));
    }

    @Test
    void constructorWithNegativeHeightThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Stage(100, -1));
    }

    // ─── Constructor(width, height, backgroundColor) ─────────────────────────────

    @Test
    void constructorWithBackgroundColorStoresDimensions() {
        Stage stage = new Stage(640, 480, 0xFFFF0000);
        assertEquals(640, stage.getWidth());
        assertEquals(480, stage.getHeight());
    }

    @Test
    void constructorWithBackgroundColorStoresColor() {
        Stage stage = new Stage(10, 10, 0xFFFF0000);
        assertEquals(0xFFFF0000, stage.getBackgroundColor());
    }

    @Test
    void constructorWithTransparentBackgroundColorIsAccepted() {
        Stage stage = new Stage(10, 10, 0x00000000);
        assertEquals(0x00000000, stage.getBackgroundColor());
    }

    @Test
    void constructorWithBackgroundColorAndZeroWidthThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Stage(0, 10, 0xFF000000));
    }

    @Test
    void constructorWithBackgroundColorAndZeroHeightThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Stage(10, 0, 0xFF000000));
    }

    // ─── getImage() ──────────────────────────────────────────────────────────────

    @Test
    void getImageReturnsNonNull() {
        assertNotNull(new Stage(10, 10).getImage());
    }

    @Test
    void getImageReturnsArgbType() {
        Stage stage = new Stage(10, 10);
        assertEquals(BufferedImage.TYPE_INT_ARGB, stage.getImage().getType());
    }

    @Test
    void getImageReturnsSameInstanceOnRepeatedCalls() {
        Stage stage = new Stage(10, 10);
        assertSame(stage.getImage(), stage.getImage());
    }

    @Test
    void getImageHasCorrectDimensions() {
        Stage stage = new Stage(80, 60);
        assertEquals(80, stage.getImage().getWidth());
        assertEquals(60, stage.getImage().getHeight());
    }

    // ─── getPixels() ─────────────────────────────────────────────────────────────

    @Test
    void getPixelsReturnsNonNull() {
        assertNotNull(new Stage(10, 10).getPixels());
    }

    @Test
    void getPixelsLengthEqualsWidthTimesHeight() {
        Stage stage = new Stage(80, 60);
        assertEquals(80 * 60, stage.getPixels().length);
    }

    @Test
    void getPixelsLengthForOneBySizeOne() {
        assertEquals(1, new Stage(1, 1).getPixels().length);
    }

    @Test
    void getPixelsReturnsSameArrayOnRepeatedCalls() {
        Stage stage = new Stage(10, 10);
        assertSame(stage.getPixels(), stage.getPixels());
    }

    @Test
    void getPixelsWriteIsReflectedInGetImage() {
        // Contract: "Modifications to this array are immediately reflected
        // in the image returned by getImage()"
        Stage stage = new Stage(4, 4);
        int opaqueCyan = 0xFF00FFFF;
        stage.getPixels()[0] = opaqueCyan;
        assertEquals(opaqueCyan, stage.getImage().getRGB(0, 0));
    }

    @Test
    void getPixelsWriteAtLastPositionIsReflectedInGetImage() {
        Stage stage = new Stage(4, 4);
        int opaqueYellow = 0xFFFFFF00;
        int lastIndex = stage.getWidth() * stage.getHeight() - 1;
        stage.getPixels()[lastIndex] = opaqueYellow;
        assertEquals(opaqueYellow, stage.getImage().getRGB(stage.getWidth() - 1, stage.getHeight() - 1));
    }

    // ─── setBackgroundColor() ────────────────────────────────────────────────────

    @Test
    void setBackgroundColorUpdatesGetBackgroundColor() {
        Stage stage = new Stage(10, 10);
        stage.setBackgroundColor(0xFF00FF00);
        assertEquals(0xFF00FF00, stage.getBackgroundColor());
    }

    @Test
    void setBackgroundColorCanBeCalledMultipleTimes() {
        Stage stage = new Stage(10, 10);
        stage.setBackgroundColor(0xFFFF0000);
        stage.setBackgroundColor(0xFF0000FF);
        assertEquals(0xFF0000FF, stage.getBackgroundColor());
    }

    @Test
    void setBackgroundColorToTransparentIsAccepted() {
        Stage stage = new Stage(10, 10);
        stage.setBackgroundColor(0x00000000);
        assertEquals(0x00000000, stage.getBackgroundColor());
    }
}