/*
 * EffectPipelineWhiteBoxTest.java
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
package fr.dufrenoy.imagefx.staging;

import java.awt.image.BufferedImage;

import fr.dufrenoy.imagefx.source.ImageSource;
import fr.dufrenoy.imagefx.source.TileMap;
import fr.dufrenoy.imagefx.source.TileSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * White-box tests for {@link EffectPipeline}.
 *
 * <p>These tests target implementation paths that the black-box suite
 * does not distinguish:</p>
 * <ul>
 *   <li>Transform chaining order (non-commutative).</li>
 *   <li>{@code prepare()} called once per render, not per pixel.</li>
 *   <li>Bilinear edge clamping for bounded sources.</li>
 *   <li>Fixed-point bilinear precision at 0.5 and 2-D midpoints.</li>
 *   <li>Parallel rendering determinism (parallel stream per y-row).</li>
 *   <li>{@code RotateTransform.apply()} saves dx/dy before updating coords.</li>
 * </ul>
 */
class EffectPipelineWhiteBoxTest {

    private static final int BLACK = 0xFF000000;
    private static final int RED   = 0xFFFF0000;
    private static final int GREEN = 0xFF00FF00;
    private static final int BLUE  = 0xFF0000FF;

    private static ImageSource grid(int[][] pixels) {
        int h = pixels.length, w = pixels[0].length;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                img.setRGB(x, y, pixels[y][x]);
        return new ImageSource(img);
    }

    private static int px(Stage stage, int x, int y) {
        return stage.getImage().getRGB(x, y);
    }

    // ─── Transform ordering ───────────────────────────────────────────────────────

    @Test
    void zoomThenScrollDiffersFromScrollThenZoom() {
        // Source 4×1 [RED, GREEN, BLUE, RED]. Zoom(2, cx=0) then scrollH(1).
        //   stage(0): zoom→0/2=0 → scroll→0+1=1 → src(1) = GREEN.
        // ScrollH(1) then zoom(2, cx=0).
        //   stage(0): scroll→0+1=1 → zoom→1/2=0.5 → bilinear(RED, GREEN) ≠ GREEN.
        ImageSource src = grid(new int[][] {{ RED, GREEN, BLUE, RED }});

        Stage zoomThenScroll = new Stage(2, 1, BLACK);
        new EffectPipeline().addSource(src)
                .zoom(new ParamDouble(2), new ParamDouble(0), new ParamDouble(0))
                .scrollH(new ParamInt(1))
                .build().render(zoomThenScroll);

        Stage scrollThenZoom = new Stage(2, 1, BLACK);
        new EffectPipeline().addSource(src)
                .scrollH(new ParamInt(1))
                .zoom(new ParamDouble(2), new ParamDouble(0), new ParamDouble(0))
                .build().render(scrollThenZoom);

        assertEquals(GREEN, px(zoomThenScroll, 0, 0));
        assertNotEquals(px(zoomThenScroll, 0, 0), px(scrollThenZoom, 0, 0));
    }

    // ─── prepare() called once per render ────────────────────────────────────────

    @Test
    void zoomParamIsReevaluatedEachRender() {
        // Build once. First render with factor=2: stage(2)→src(1)=GREEN.
        // Change factor to 1. Second render: stage(2)→src(2)=BLUE.
        ParamDouble factor = new ParamDouble(2.0);
        ImageSource src = grid(new int[][] {{ RED, GREEN, BLUE }});
        Stage stage = new Stage(3, 1, BLACK);

        EffectPipeline p = new EffectPipeline()
                .addSource(src)
                .zoom(factor, new ParamDouble(0), new ParamDouble(0))
                .build();

        p.render(stage);
        assertEquals(GREEN, px(stage, 2, 0)); // factor=2: stage(2)→src(1)

        factor.set(1.0);
        p.render(stage);
        assertEquals(BLUE, px(stage, 2, 0));  // factor=1: stage(2)→src(2)
    }

    @Test
    void rotateAngleIsReevaluatedEachRender() {
        // Source 3×1 [RED, GREEN, BLUE] WRAP. Centre (1, 0).
        // Angle=0: identity; stage(2)→BLUE.
        // Angle=π: 180° flip; stage(2)→RED.
        BufferedImage sheet = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        sheet.setRGB(0, 0, RED); sheet.setRGB(1, 0, GREEN); sheet.setRGB(2, 0, BLUE);
        TileSet ts = new TileSet(sheet, 1, 1);
        TileMap map = new TileMap(ts, 3, 1, TileMap.EdgePolicy.WRAP);
        map.setTiles(0, 0, new int[][] {{ 0, 1, 2 }});

        ParamDouble angle = new ParamDouble(0);
        Stage stage = new Stage(3, 1, BLACK);

        EffectPipeline p = new EffectPipeline()
                .addSource(map)
                .rotate(angle, new ParamDouble(1), new ParamDouble(0))
                .build();

        p.render(stage);
        assertEquals(BLUE, px(stage, 2, 0)); // identity

        angle.set(Math.PI);
        p.render(stage);
        assertEquals(RED, px(stage, 2, 0));  // flipped
    }

    // ─── Bilinear edge clamping ───────────────────────────────────────────────────

    @Test
    void bilinearClampsBoundaryNeighbourForBoundedSource() {
        // Source 2×1 [RED, BLUE]. zoom(2, cx=0). Stage 4×1.
        // stage(3) → srcX=1.5 → x0=1 (in bounds), x1 clamped to 1 → bilinear(BLUE,BLUE)=BLUE.
        // Without clamping, x1=2 would call getPixel(2,0) → IndexOutOfBoundsException.
        Stage stage = new Stage(4, 1, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {{ RED, BLUE }}))
                .zoom(new ParamDouble(2.0), new ParamDouble(0), new ParamDouble(0))
                .build()
                .render(stage);
        assertEquals(BLUE, px(stage, 3, 0));
    }

    @Test
    void bilinearMixesColorsAtFractionalPosition() {
        // stage(1) → srcX=0.5 → bilinear(RED, BLUE) = mix.
        // Verifies that fractional positions are NOT nearest-neighbour (not RED, not BLUE).
        Stage stage = new Stage(4, 1, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {{ RED, BLUE }}))
                .zoom(new ParamDouble(2.0), new ParamDouble(0), new ParamDouble(0))
                .build()
                .render(stage);
        assertNotEquals(RED,  px(stage, 1, 0));
        assertNotEquals(BLUE, px(stage, 1, 0));
    }

    @Test
    void integerSourceCoordinatesUseNearestNeighbourPath() {
        // scrollH produces exact integer srcX — no bilinear mix.
        // stage(0) → src(0)=RED, stage(1) → src(1)=BLUE. No interpolation.
        Stage stage = new Stage(2, 1, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {{ RED, BLUE }}))
                .scrollH(new ParamInt(0))
                .build()
                .render(stage);
        assertEquals(RED,  px(stage, 0, 0));
        assertEquals(BLUE, px(stage, 1, 0));
    }

    // ─── Fixed-point bilinear precision ──────────────────────────────────────────

    @Test
    void bilinearAtExactHalfpointGivesMidpointChannel() {
        // zoom(2, cx=0) on [RED=0xFFFF0000, BLUE=0xFF0000FF]. stage(1) → srcX=0.5.
        // ftx=128, itx=128, fty=0, ity=256.
        // r-channel: (255*128)*256 + 32768 >> 16 = 8388608 >> 16 = 128.
        // b-channel: (255*128)*256 + 32768 >> 16 = 128.
        // Expected: 0xFF800080.
        Stage stage = new Stage(4, 1, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {{ RED, BLUE }}))
                .zoom(new ParamDouble(2.0), new ParamDouble(0), new ParamDouble(0))
                .build()
                .render(stage);
        assertEquals(0xFF800080, px(stage, 1, 0));
    }

    @Test
    void bilinearAtExact2DHalfpointGivesAverageOfFourCorners() {
        // 2×2 source: RED, BLUE top row; GREEN, BLACK bottom row.
        // zoom(2, cx=0, cy=0), stage 4×4. stage(1,1) → srcX=0.5, srcY=0.5.
        // All four corners contribute equally (weight 0.25 each).
        // r: round((255+0+0+0)/4) = 64. g: round((0+0+255+0)/4) = 64. b: round((0+255+0+0)/4) = 64.
        Stage stage = new Stage(4, 4, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {
                        { RED,   BLUE  },
                        { GREEN, BLACK }
                }))
                .zoom(new ParamDouble(2.0), new ParamDouble(0), new ParamDouble(0))
                .build()
                .render(stage);
        int pixel = px(stage, 1, 1);
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >>  8) & 0xFF;
        int b =  pixel        & 0xFF;
        // Fixed-point rounding: (255*128*128 + 32768) >> 16 = 8421248 >> 16 = 128...
        // Actually with four equal-weight corners: each weight = 128*128 = 16384 out of 65536.
        // r = (255*16384 + 0 + 0 + 0 + 32768) >> 16 = (4194048 + 32768) >> 16 = 4226816 >> 16 = 64.
        assertEquals(64, r, 1); // tolerance 1 for fixed-point rounding
        assertEquals(64, g, 1);
        assertEquals(64, b, 1);
    }

    // ─── Parallel rendering determinism ──────────────────────────────────────────

    @Test
    void parallelRenderProducesDeterministicResults() {
        // Parallel streams must produce identical pixel values on every call.
        // Uses zoom + rotate to exercise the bilinear path across many pixels.
        BufferedImage sheet = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        int[] colors = { RED, GREEN, BLUE, BLACK };
        for (int y = 0; y < 4; y++)
            for (int x = 0; x < 4; x++)
                sheet.setRGB(x, y, colors[(x + y) % 4]);
        TileSet ts = new TileSet(sheet, 1, 1);
        TileMap map = new TileMap(ts, 4, 4, TileMap.EdgePolicy.WRAP);
        int[][] tiles = { {0,1,2,3}, {1,2,3,0}, {2,3,0,1}, {3,0,1,2} };
        map.setTiles(0, 0, tiles);

        EffectPipeline pipeline = new EffectPipeline()
                .addSource(map)
                .zoom(new ParamDouble(1.3))
                .rotate(new ParamDouble(0.7))
                .build();

        Stage reference = new Stage(16, 16, BLACK);
        pipeline.render(reference);
        int[] refPixels = reference.getPixels().clone();

        for (int i = 0; i < 4; i++) {
            Stage stage = new Stage(16, 16, BLACK);
            pipeline.render(stage);
            int[] got = stage.getPixels();
            for (int p = 0; p < got.length; p++) {
                assertEquals(refPixels[p], got[p],
                        "pixel mismatch at index " + p + " on run " + i);
            }
        }
    }

    // ─── RotateTransform: dx/dy captured before c[0] update ──────────────────────

    @Test
    void rotateApplyUsesOriginalCoordsToDerivesBothOutputs() {
        // 90° CW rotation around (1,1): srcX = 1 + dx*0 + dy*1 = 1 + dy,
        //                               srcY = 1 - dx*1 + dy*0 = 1 - dx.
        // stage(2,1): dx=1, dy=0 → srcX=1, srcY=0 → GREEN.
        // stage(1,2): dx=0, dy=1 → srcX=2, srcY=1 → GREEN.
        // If c[0] were updated before c[1] used it, srcY would use the wrong value.
        BufferedImage sheet = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        int[][] src = {
                { RED,   GREEN, BLUE  },
                { BLACK, RED,   GREEN },
                { BLUE,  BLACK, RED   }
        };
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 3; x++)
                sheet.setRGB(x, y, src[y][x]);
        TileSet ts = new TileSet(sheet, 1, 1);
        TileMap map = new TileMap(ts, 3, 3, TileMap.EdgePolicy.WRAP);
        int[][] tiles = { {0,1,2}, {3,4,5}, {6,7,8} };
        map.setTiles(0, 0, tiles);

        Stage stage = new Stage(3, 3, BLACK);
        new EffectPipeline()
                .addSource(map)
                .rotate(new ParamDouble(Math.PI / 2),
                        new ParamDouble(1), new ParamDouble(1))
                .build()
                .render(stage);

        // stage(2,1): dx=1, dy=0 → srcX=1, srcY=0 → GREEN
        assertEquals(GREEN, px(stage, 2, 1));
        // stage(1,2): dx=0, dy=1 → srcX=2, srcY=1 → GREEN
        assertEquals(GREEN, px(stage, 1, 2));
    }
}