/*
 * WrappingImageSourceBlackBoxTest.java
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Black-box tests for {@link WrappingImageSource}.
 */
class WrappingImageSourceBlackBoxTest {

    private static final int RED  = 0xFFFF0000;
    private static final int BLUE = 0xFF0000FF;

    /** 2×1 image: pixel(0,0)=RED, pixel(1,0)=BLUE. */
    private static BufferedImage twoByOne() {
        BufferedImage img = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, RED);
        img.setRGB(1, 0, BLUE);
        return img;
    }

    /** 1×2 image: pixel(0,0)=RED, pixel(0,1)=BLUE. */
    private static BufferedImage oneByTwo() {
        BufferedImage img = new BufferedImage(1, 2, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, RED);
        img.setRGB(0, 1, BLUE);
        return img;
    }

    // ─── Constructor ─────────────────────────────────────────────────────────────

    @Test
    void nullImageThrows() {
        assertThrows(NullPointerException.class, () -> new WrappingImageSource(null));
    }

    @Test
    void getWidthMatchesImage() {
        assertEquals(2, new WrappingImageSource(twoByOne()).getWidth());
    }

    @Test
    void getHeightMatchesImage() {
        assertEquals(2, new WrappingImageSource(oneByTwo()).getHeight());
    }

    // ─── isUnbounded ─────────────────────────────────────────────────────────────

    @Test
    void isUnboundedReturnsTrue() {
        assertTrue(new WrappingImageSource(twoByOne()).isUnbounded());
    }

    // ─── getPixel — in-bounds ─────────────────────────────────────────────────────

    @Test
    void getPixelInBoundsReturnsCorrectColor() {
        WrappingImageSource src = new WrappingImageSource(twoByOne());
        assertEquals(RED,  src.getPixel(0, 0));
        assertEquals(BLUE, src.getPixel(1, 0));
    }

    // ─── getPixel — horizontal wrap ───────────────────────────────────────────────

    @Test
    void getPixelWrapsPositiveX() {
        WrappingImageSource src = new WrappingImageSource(twoByOne());
        assertEquals(RED,  src.getPixel(2, 0)); // 2 % 2 = 0
        assertEquals(BLUE, src.getPixel(3, 0)); // 3 % 2 = 1
    }

    @Test
    void getPixelWrapsNegativeX() {
        WrappingImageSource src = new WrappingImageSource(twoByOne());
        assertEquals(BLUE, src.getPixel(-1, 0)); // floorMod(-1, 2) = 1
        assertEquals(RED,  src.getPixel(-2, 0)); // floorMod(-2, 2) = 0
    }

    // ─── getPixel — vertical wrap ─────────────────────────────────────────────────

    @Test
    void getPixelWrapsPositiveY() {
        WrappingImageSource src = new WrappingImageSource(oneByTwo());
        assertEquals(RED,  src.getPixel(0, 2)); // 2 % 2 = 0
        assertEquals(BLUE, src.getPixel(0, 3)); // 3 % 2 = 1
    }

    @Test
    void getPixelWrapsNegativeY() {
        WrappingImageSource src = new WrappingImageSource(oneByTwo());
        assertEquals(BLUE, src.getPixel(0, -1)); // floorMod(-1, 2) = 1
        assertEquals(RED,  src.getPixel(0, -2)); // floorMod(-2, 2) = 0
    }

    // ─── Non-ARGB source image ────────────────────────────────────────────────────

    @Test
    void nonArgbImageIsConvertedTransparently() {
        BufferedImage rgb = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        rgb.setRGB(0, 0, 0xFF123456);
        WrappingImageSource src = new WrappingImageSource(rgb);
        // After conversion to ARGB the pixel is fully opaque.
        assertEquals(0xFF123456, src.getPixel(0, 0));
    }
}