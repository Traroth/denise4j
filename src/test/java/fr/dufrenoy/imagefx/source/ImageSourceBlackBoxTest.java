/*
 * ImageSourceBlackBoxTest.java
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

/**
 * Black-box tests for {@link ImageSource}.
 *
 * <p>These tests verify the public contract of {@code ImageSource} as
 * specified by its Javadoc and JML annotations, with no knowledge
 * of internal implementation.</p>
 */
class ImageSourceBlackBoxTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private static BufferedImage argbImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    // ─── Constructor ─────────────────────────────────────────────────────────────

    @Test
    void constructorStoresDimensions() {
        BufferedImage img = argbImage(80, 60);
        ImageSource src = new ImageSource(img);
        assertEquals(80, src.getWidth());
        assertEquals(60, src.getHeight());
    }

    @Test
    void constructorWithOnePxByOnePxImageIsAccepted() {
        ImageSource src = new ImageSource(argbImage(1, 1));
        assertEquals(1, src.getWidth());
        assertEquals(1, src.getHeight());
    }

    @Test
    void constructorWithNullImageThrows() {
        assertThrows(NullPointerException.class, () -> new ImageSource(null));
    }

    // ─── getWidth() / getHeight() ─────────────────────────────────────────────────

    @Test
    void getWidthMatchesImageWidth() {
        assertEquals(123, new ImageSource(argbImage(123, 1)).getWidth());
    }

    @Test
    void getHeightMatchesImageHeight() {
        assertEquals(77, new ImageSource(argbImage(1, 77)).getHeight());
    }

    // ─── getPixel() — reading correct pixels ──────────────────────────────────────

    @Test
    void getPixelReturnsCorrectArgbAtTopLeft() {
        BufferedImage img = argbImage(4, 4);
        img.setRGB(0, 0, 0xFFFF0000);
        ImageSource src = new ImageSource(img);
        assertEquals(0xFFFF0000, src.getPixel(0, 0));
    }

    @Test
    void getPixelReturnsCorrectArgbAtBottomRight() {
        BufferedImage img = argbImage(4, 4);
        img.setRGB(3, 3, 0xFF0000FF);
        ImageSource src = new ImageSource(img);
        assertEquals(0xFF0000FF, src.getPixel(3, 3));
    }

    @Test
    void getPixelReturnsCorrectArgbAtArbitraryPosition() {
        BufferedImage img = argbImage(10, 10);
        img.setRGB(5, 7, 0xFF00FF00);
        ImageSource src = new ImageSource(img);
        assertEquals(0xFF00FF00, src.getPixel(5, 7));
    }

    @Test
    void getPixelOnSinglePixelImage() {
        BufferedImage img = argbImage(1, 1);
        img.setRGB(0, 0, 0xFFABCDEF);
        ImageSource src = new ImageSource(img);
        assertEquals(0xFFABCDEF, src.getPixel(0, 0));
    }

    @Test
    void getPixelReturnsTransparentForUnsetPixel() {
        // A fresh TYPE_INT_ARGB image is initialised to 0x00000000
        ImageSource src = new ImageSource(argbImage(4, 4));
        assertEquals(0x00000000, src.getPixel(2, 2));
    }

    @Test
    void getPixelWorksWithTypeIntRgbImage() {
        // TYPE_INT_RGB images have no alpha; getRGB always returns 0xFF in alpha
        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, 0x00FF0000); // alpha ignored for TYPE_INT_RGB
        ImageSource src = new ImageSource(img);
        // BufferedImage.getRGB() returns 0xFF000000 | rgb for opaque types
        assertEquals(0xFFFF0000, src.getPixel(0, 0));
    }

    // ─── getPixel() — out-of-bounds ───────────────────────────────────────────────

    @Test
    void getPixelWithNegativeXThrows() {
        ImageSource src = new ImageSource(argbImage(4, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> src.getPixel(-1, 0));
    }

    @Test
    void getPixelWithNegativeYThrows() {
        ImageSource src = new ImageSource(argbImage(4, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> src.getPixel(0, -1));
    }

    @Test
    void getPixelWithXEqualToWidthThrows() {
        ImageSource src = new ImageSource(argbImage(4, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> src.getPixel(4, 0));
    }

    @Test
    void getPixelWithYEqualToHeightThrows() {
        ImageSource src = new ImageSource(argbImage(4, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> src.getPixel(0, 4));
    }
}