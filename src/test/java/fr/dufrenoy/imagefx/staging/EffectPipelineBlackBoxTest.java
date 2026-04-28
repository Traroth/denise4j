/*
 * EffectPipelineBlackBoxTest.java
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Black-box tests for {@link EffectPipeline}.
 *
 * <p>Tests are organised into two sections: the builder lifecycle
 * (state machine) and the pixel-level render behaviour.</p>
 *
 * <h2>Compositing model</h2>
 * <p>Always-overwrite: if the source covers a stage position and the
 * pixel does not match the layer's transparent colour, it overwrites the
 * stage pixel. Otherwise the stage pixel (background colour or previous
 * layer) is left unchanged.</p>
 */
class EffectPipelineBlackBoxTest {

    private static final int BLACK   = 0xFF000000;
    private static final int RED     = 0xFFFF0000;
    private static final int GREEN   = 0xFF00FF00;
    private static final int BLUE    = 0xFF0000FF;
    private static final int MAGENTA = 0xFFFF00FF;

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    /** Returns a 1×1 ImageSource filled with the given colour. */
    private static ImageSource solid(int width, int height, int argb) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, argb);
            }
        }
        return new ImageSource(img);
    }

    /**
     * Returns an ImageSource backed by the given pixel grid.
     * {@code pixels[y][x]} is the ARGB value at column x, row y.
     */
    private static ImageSource grid(int[][] pixels) {
        int h = pixels.length;
        int w = pixels[0].length;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, pixels[y][x]);
            }
        }
        return new ImageSource(img);
    }

    /** Returns the ARGB pixel at (x, y) of the stage's image. */
    private static int px(Stage stage, int x, int y) {
        return stage.getImage().getRGB(x, y);
    }

    // ─── Lifecycle — constructor ──────────────────────────────────────────────────

    @Test
    void constructorIsNotBuilt() {
        assertFalse(new EffectPipeline().isBuilt());
    }

    @Test
    void constructorHasNoSource() {
        assertFalse(new EffectPipeline().hasAtLeastOneSource());
    }

    // ─── Lifecycle — addSource() ──────────────────────────────────────────────────

    @Test
    void addSourceSetsHasAtLeastOneSource() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED));
        assertTrue(p.hasAtLeastOneSource());
    }

    @Test
    void addSourceReturnsThis() {
        EffectPipeline p = new EffectPipeline();
        assertSame(p, p.addSource(solid(1, 1, RED)));
    }

    @Test
    void addSourceWithNullThrows() {
        assertThrows(NullPointerException.class,
                () -> new EffectPipeline().addSource(null));
    }

    @Test
    void addSourceAfterBuildThrows() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED)).build();
        assertThrows(IllegalStateException.class,
                () -> p.addSource(solid(1, 1, GREEN)));
    }

    // ─── Lifecycle — transparentColor() ──────────────────────────────────────────

    @Test
    void transparentColorReturnsThis() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED));
        assertSame(p, p.transparentColor(MAGENTA));
    }

    @Test
    void transparentColorBeforeAnySourceThrows() {
        assertThrows(IllegalStateException.class,
                () -> new EffectPipeline().transparentColor(MAGENTA));
    }

    @Test
    void transparentColorAfterBuildThrows() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED)).build();
        assertThrows(IllegalStateException.class, () -> p.transparentColor(MAGENTA));
    }

    // ─── Lifecycle — fuseLayer() ──────────────────────────────────────────────────

    @Test
    void fuseLayerReturnsThis() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED));
        assertSame(p, p.fuseLayer());
    }

    @Test
    void fuseLayerBeforeAnySourceThrows() {
        assertThrows(IllegalStateException.class,
                () -> new EffectPipeline().fuseLayer());
    }

    @Test
    void fuseLayerAfterBuildThrows() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED)).build();
        assertThrows(IllegalStateException.class, () -> p.fuseLayer());
    }

    @Test
    void fuseLayerDoesNotResetHasAtLeastOneSource() {
        EffectPipeline p = new EffectPipeline()
                .addSource(solid(1, 1, RED))
                .fuseLayer();
        assertTrue(p.hasAtLeastOneSource());
    }

    // ─── Lifecycle — build() ─────────────────────────────────────────────────────

    @Test
    void buildSetsIsBuilt() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED)).build();
        assertTrue(p.isBuilt());
    }

    @Test
    void buildReturnsThis() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED));
        assertSame(p, p.build());
    }

    @Test
    void buildWithoutSourceThrows() {
        assertThrows(IllegalStateException.class,
                () -> new EffectPipeline().build());
    }

    @Test
    void buildTwiceThrows() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED)).build();
        assertThrows(IllegalStateException.class, () -> p.build());
    }

    // ─── render() — lifecycle errors ─────────────────────────────────────────────

    @Test
    void renderBeforeBuildThrows() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED));
        assertThrows(IllegalStateException.class, () -> p.render(new Stage(1, 1)));
    }

    @Test
    void renderWithNullStageThrows() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED)).build();
        assertThrows(NullPointerException.class, () -> p.render(null));
    }

    // ─── render() — backgroundColor ──────────────────────────────────────────────

    @Test
    void renderFillsAreaOutsideSourceWithBackgroundColor() {
        // Source is 2×1; stage is 4×1. Pixels at x=2 and x=3 have no source
        // coverage and must show the background colour.
        Stage stage = new Stage(4, 1, RED);
        new EffectPipeline()
                .addSource(solid(2, 1, GREEN))
                .build()
                .render(stage);
        assertEquals(GREEN, px(stage, 0, 0));
        assertEquals(GREEN, px(stage, 1, 0));
        assertEquals(RED,   px(stage, 2, 0)); // background
        assertEquals(RED,   px(stage, 3, 0)); // background
    }

    @Test
    void renderClearsEntireStageBeforeCompositing() {
        // Render twice: first with a green source, then with an empty pipeline
        // that covers only x=0. x=1 must show the current background, not the
        // leftover green from the first render.
        Stage stage = new Stage(2, 1, BLACK);
        EffectPipeline firstRender = new EffectPipeline()
                .addSource(solid(2, 1, GREEN))
                .build();
        firstRender.render(stage);

        EffectPipeline secondRender = new EffectPipeline()
                .addSource(solid(1, 1, RED))
                .build();
        secondRender.render(stage);

        assertEquals(RED,   px(stage, 0, 0));
        assertEquals(BLACK, px(stage, 1, 0)); // cleared to background, not leftover green
    }

    // ─── render() — single opaque source ─────────────────────────────────────────

    @Test
    void renderSingleOpaqueSameSizeSourceOverwritesWholeStage() {
        Stage stage = new Stage(2, 2, BLACK);
        new EffectPipeline()
                .addSource(solid(2, 2, BLUE))
                .build()
                .render(stage);
        assertEquals(BLUE, px(stage, 0, 0));
        assertEquals(BLUE, px(stage, 1, 0));
        assertEquals(BLUE, px(stage, 0, 1));
        assertEquals(BLUE, px(stage, 1, 1));
    }

    @Test
    void renderPreservesSourcePixelValues() {
        Stage stage = new Stage(2, 2, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {
                        { RED,  GREEN },
                        { BLUE, BLACK }
                }))
                .build()
                .render(stage);
        assertEquals(RED,   px(stage, 0, 0));
        assertEquals(GREEN, px(stage, 1, 0));
        assertEquals(BLUE,  px(stage, 0, 1));
        assertEquals(BLACK, px(stage, 1, 1));
    }

    @Test
    void renderCanBeCalledMultipleTimes() {
        Stage stage = new Stage(1, 1, BLACK);
        EffectPipeline p = new EffectPipeline()
                .addSource(solid(1, 1, RED))
                .build();
        p.render(stage);
        assertEquals(RED, px(stage, 0, 0));
        p.render(stage);
        assertEquals(RED, px(stage, 0, 0));
    }

    // ─── render() — transparentColor ─────────────────────────────────────────────

    @Test
    void renderTransparentColorPixelsShowBackground() {
        // Pixel (0,0) is magenta (= transparent colour); must show background.
        Stage stage = new Stage(2, 1, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {{ MAGENTA, RED }}))
                .transparentColor(MAGENTA)
                .build()
                .render(stage);
        assertEquals(BLACK, px(stage, 0, 0)); // transparent → background
        assertEquals(RED,   px(stage, 1, 0));
    }

    @Test
    void renderTransparentColorOnSecondLayerShowsFirstLayer() {
        // Layer 1: all blue. Layer 2: (0,0)=magenta, rest=red, key=magenta.
        // Magenta pixels on layer 2 let layer 1 (blue) show through.
        Stage stage = new Stage(2, 2, BLACK);
        new EffectPipeline()
                .addSource(solid(2, 2, BLUE))
                .addSource(grid(new int[][] {
                        { MAGENTA, RED },
                        { RED,     RED }
                }))
                .transparentColor(MAGENTA)
                .build()
                .render(stage);
        assertEquals(BLUE, px(stage, 0, 0)); // magenta → layer 1 (blue) shows
        assertEquals(RED,  px(stage, 1, 0));
        assertEquals(RED,  px(stage, 0, 1));
        assertEquals(RED,  px(stage, 1, 1));
    }

    // ─── render() — two layers ────────────────────────────────────────────────────

    @Test
    void renderSecondLayerOverwritesFirstWhereNonTransparent() {
        // Layer 1: all blue. Layer 2: all red. No transparent colour.
        // Layer 2 must overwrite layer 1 everywhere.
        Stage stage = new Stage(2, 2, BLACK);
        new EffectPipeline()
                .addSource(solid(2, 2, BLUE))
                .addSource(solid(2, 2, RED))
                .build()
                .render(stage);
        assertEquals(RED, px(stage, 0, 0));
        assertEquals(RED, px(stage, 1, 0));
        assertEquals(RED, px(stage, 0, 1));
        assertEquals(RED, px(stage, 1, 1));
    }

    @Test
    void renderSecondLayerSmallerThanFirstShowsFirstOutsideBounds() {
        // Layer 1: 2×2 all blue. Layer 2: 1×1 red (covers only top-left).
        Stage stage = new Stage(2, 2, BLACK);
        new EffectPipeline()
                .addSource(solid(2, 2, BLUE))
                .addSource(solid(1, 1, RED))
                .build()
                .render(stage);
        assertEquals(RED,  px(stage, 0, 0)); // layer 2 covers
        assertEquals(BLUE, px(stage, 1, 0)); // layer 2 does not cover → layer 1
        assertEquals(BLUE, px(stage, 0, 1));
        assertEquals(BLUE, px(stage, 1, 1));
    }

    // ─── render() — unbounded source (WRAP) ──────────────────────────────────────

    @Test
    void renderUnboundedSourceWrapsCoordinatesAcrossStageWidth() {
        // TileMap 2×1 (red, green), WRAP. Stage 4×1.
        // Pipeline reads x=0→red, x=1→green, x=2→red (wrap), x=3→green (wrap).
        BufferedImage sheet = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        sheet.setRGB(0, 0, RED);
        sheet.setRGB(1, 0, GREEN);
        TileSet ts = new TileSet(sheet, 1, 1);
        TileMap map = new TileMap(ts, 2, 1, TileMap.EdgePolicy.WRAP);
        map.setTiles(0, 0, new int[][] {{ 0, 1 }});

        Stage stage = new Stage(4, 1, BLACK);
        new EffectPipeline()
                .addSource(map)
                .build()
                .render(stage);

        assertEquals(RED,   px(stage, 0, 0));
        assertEquals(GREEN, px(stage, 1, 0));
        assertEquals(RED,   px(stage, 2, 0)); // wrapped
        assertEquals(GREEN, px(stage, 3, 0)); // wrapped
    }

    @Test
    void renderBoundedSourceDoesNotWrap() {
        // Same TileMap but CLIP. Stage 4×1.
        // x=2 and x=3 are outside source bounds → background shows.
        BufferedImage sheet = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        sheet.setRGB(0, 0, RED);
        sheet.setRGB(1, 0, GREEN);
        TileSet ts = new TileSet(sheet, 1, 1);
        TileMap map = new TileMap(ts, 2, 1, TileMap.EdgePolicy.CLIP);
        map.setTiles(0, 0, new int[][] {{ 0, 1 }});

        Stage stage = new Stage(4, 1, BLACK);
        new EffectPipeline()
                .addSource(map)
                .build()
                .render(stage);

        assertEquals(RED,   px(stage, 0, 0));
        assertEquals(GREEN, px(stage, 1, 0));
        assertEquals(BLACK, px(stage, 2, 0)); // background
        assertEquals(BLACK, px(stage, 3, 0)); // background
    }

    // ─── render() — fuseLayer ────────────────────────────────────────────────────

    @Test
    void renderFuseLayerSingleSourceProducesSameResultAsWithoutFuse() {
        Stage withFuse    = new Stage(2, 2, BLACK);
        Stage withoutFuse = new Stage(2, 2, BLACK);

        new EffectPipeline()
                .addSource(solid(2, 2, BLUE))
                .fuseLayer()
                .build()
                .render(withFuse);

        new EffectPipeline()
                .addSource(solid(2, 2, BLUE))
                .build()
                .render(withoutFuse);

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                assertEquals(px(withoutFuse, x, y), px(withFuse, x, y),
                        "Pixel mismatch at (" + x + ", " + y + ")");
            }
        }
    }

    @Test
    void renderFuseLayerThenAddSourceCompositesProperly() {
        // Layer 1: all blue. fuseLayer(). Layer 2: (0,0)=magenta, rest=red, key=magenta.
        // Magenta pixels on layer 2 show the fused result (blue).
        Stage stage = new Stage(2, 2, BLACK);
        new EffectPipeline()
                .addSource(solid(2, 2, BLUE))
                .fuseLayer()
                .addSource(grid(new int[][] {
                        { MAGENTA, RED },
                        { RED,     RED }
                }))
                .transparentColor(MAGENTA)
                .build()
                .render(stage);

        assertEquals(BLUE, px(stage, 0, 0)); // magenta → fused (blue) shows
        assertEquals(RED,  px(stage, 1, 0));
        assertEquals(RED,  px(stage, 0, 1));
        assertEquals(RED,  px(stage, 1, 1));
    }

    // ─── scrollH() — lifecycle ────────────────────────────────────────────────────

    @Test
    void scrollHReturnsThis() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED));
        assertSame(p, p.scrollH(new ParamInt(0)));
    }

    @Test
    void scrollHWithNullOffsetThrows() {
        assertThrows(NullPointerException.class,
                () -> new EffectPipeline().addSource(solid(1, 1, RED)).scrollH(null));
    }

    @Test
    void scrollHBeforeAnySourceThrows() {
        assertThrows(IllegalStateException.class,
                () -> new EffectPipeline().scrollH(new ParamInt(0)));
    }

    @Test
    void scrollHAfterBuildThrows() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED)).build();
        assertThrows(IllegalStateException.class, () -> p.scrollH(new ParamInt(0)));
    }

    // ─── scrollV() — lifecycle ────────────────────────────────────────────────────

    @Test
    void scrollVReturnsThis() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED));
        assertSame(p, p.scrollV(new ParamInt(0)));
    }

    @Test
    void scrollVWithNullOffsetThrows() {
        assertThrows(NullPointerException.class,
                () -> new EffectPipeline().addSource(solid(1, 1, RED)).scrollV(null));
    }

    @Test
    void scrollVBeforeAnySourceThrows() {
        assertThrows(IllegalStateException.class,
                () -> new EffectPipeline().scrollV(new ParamInt(0)));
    }

    @Test
    void scrollVAfterBuildThrows() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED)).build();
        assertThrows(IllegalStateException.class, () -> p.scrollV(new ParamInt(0)));
    }

    // ─── scrollH() — render behaviour ────────────────────────────────────────────

    @Test
    void renderScrollHZeroIsIdentity() {
        // offset=0 must produce the same result as no scroll.
        Stage withScroll    = new Stage(3, 1, BLACK);
        Stage withoutScroll = new Stage(3, 1, BLACK);
        ImageSource src = grid(new int[][] {{ RED, GREEN, BLUE }});

        new EffectPipeline().addSource(src).scrollH(new ParamInt(0)).build().render(withScroll);
        new EffectPipeline().addSource(src).build().render(withoutScroll);

        assertEquals(px(withoutScroll, 0, 0), px(withScroll, 0, 0));
        assertEquals(px(withoutScroll, 1, 0), px(withScroll, 1, 0));
        assertEquals(px(withoutScroll, 2, 0), px(withScroll, 2, 0));
    }

    @Test
    void renderScrollHPositiveOffsetReadsFromFurtherRight() {
        // Source 4×1: [RED, GREEN, BLUE, RED]. Stage 3×1. offset=1.
        // stage(x) reads source(x+1): RED→GREEN, GREEN→BLUE, BLUE→RED.
        Stage stage = new Stage(3, 1, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {{ RED, GREEN, BLUE, RED }}))
                .scrollH(new ParamInt(1))
                .build()
                .render(stage);
        assertEquals(GREEN, px(stage, 0, 0));
        assertEquals(BLUE,  px(stage, 1, 0));
        assertEquals(RED,   px(stage, 2, 0));
    }

    @Test
    void renderScrollHNegativeOffsetReadsFromFurtherLeft() {
        // Source 3×1: [RED, GREEN, BLUE]. Stage 4×1. offset=-1.
        // stage(0) → src(-1) OOB → BLACK; stage(1)→src(0)=RED; etc.
        Stage stage = new Stage(4, 1, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {{ RED, GREEN, BLUE }}))
                .scrollH(new ParamInt(-1))
                .build()
                .render(stage);
        assertEquals(BLACK, px(stage, 0, 0)); // src(-1) out of bounds
        assertEquals(RED,   px(stage, 1, 0));
        assertEquals(GREEN, px(stage, 2, 0));
        assertEquals(BLUE,  px(stage, 3, 0));
    }

    @Test
    void renderScrollHBoundedSourceOOBShowsBackground() {
        // Source 3×1: [RED, GREEN, BLUE]. Stage 4×1. offset=2.
        // stage(0)→src(2)=BLUE; stage(1..3)→src(3..5) OOB → BLACK.
        Stage stage = new Stage(4, 1, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {{ RED, GREEN, BLUE }}))
                .scrollH(new ParamInt(2))
                .build()
                .render(stage);
        assertEquals(BLUE,  px(stage, 0, 0));
        assertEquals(BLACK, px(stage, 1, 0));
        assertEquals(BLACK, px(stage, 2, 0));
        assertEquals(BLACK, px(stage, 3, 0));
    }

    @Test
    void renderScrollHUnboundedSourceWraps() {
        // TileMap 2×1 [RED, GREEN], WRAP. Stage 4×1. offset=1.
        // stage(0)→src(1)=GREEN; stage(1)→src(2)→wrap→RED; etc.
        BufferedImage sheet = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        sheet.setRGB(0, 0, RED);
        sheet.setRGB(1, 0, GREEN);
        TileSet ts = new TileSet(sheet, 1, 1);
        TileMap map = new TileMap(ts, 2, 1, TileMap.EdgePolicy.WRAP);
        map.setTiles(0, 0, new int[][] {{ 0, 1 }});

        Stage stage = new Stage(4, 1, BLACK);
        new EffectPipeline()
                .addSource(map)
                .scrollH(new ParamInt(1))
                .build()
                .render(stage);

        assertEquals(GREEN, px(stage, 0, 0));
        assertEquals(RED,   px(stage, 1, 0)); // src(2) → wrap → 0 → RED
        assertEquals(GREEN, px(stage, 2, 0)); // src(3) → wrap → 1 → GREEN
        assertEquals(RED,   px(stage, 3, 0)); // src(4) → wrap → 0 → RED
    }

    // ─── scrollV() — render behaviour ────────────────────────────────────────────

    @Test
    void renderScrollVZeroIsIdentity() {
        Stage withScroll    = new Stage(1, 3, BLACK);
        Stage withoutScroll = new Stage(1, 3, BLACK);
        ImageSource src = grid(new int[][] {{ RED }, { GREEN }, { BLUE }});

        new EffectPipeline().addSource(src).scrollV(new ParamInt(0)).build().render(withScroll);
        new EffectPipeline().addSource(src).build().render(withoutScroll);

        assertEquals(px(withoutScroll, 0, 0), px(withScroll, 0, 0));
        assertEquals(px(withoutScroll, 0, 1), px(withScroll, 0, 1));
        assertEquals(px(withoutScroll, 0, 2), px(withScroll, 0, 2));
    }

    @Test
    void renderScrollVPositiveOffsetReadsFromFurtherDown() {
        // Source 1×4: [RED, GREEN, BLUE, RED]. Stage 1×3. offset=1.
        // stage(0,y) reads source(0, y+1).
        Stage stage = new Stage(1, 3, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {{ RED }, { GREEN }, { BLUE }, { RED }}))
                .scrollV(new ParamInt(1))
                .build()
                .render(stage);
        assertEquals(GREEN, px(stage, 0, 0));
        assertEquals(BLUE,  px(stage, 0, 1));
        assertEquals(RED,   px(stage, 0, 2));
    }

    @Test
    void renderScrollVNegativeOffsetReadsFromFurtherUp() {
        // Source 1×3: [RED, GREEN, BLUE]. Stage 1×4. offset=-1.
        // stage(0,0)→src(0,-1) OOB → BLACK; stage(0,1)→src(0,0)=RED; etc.
        Stage stage = new Stage(1, 4, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {{ RED }, { GREEN }, { BLUE }}))
                .scrollV(new ParamInt(-1))
                .build()
                .render(stage);
        assertEquals(BLACK, px(stage, 0, 0));
        assertEquals(RED,   px(stage, 0, 1));
        assertEquals(GREEN, px(stage, 0, 2));
        assertEquals(BLUE,  px(stage, 0, 3));
    }

    @Test
    void renderScrollVBoundedSourceOOBShowsBackground() {
        // Source 1×3: [RED, GREEN, BLUE]. Stage 1×4. offset=2.
        // stage(0,0)→src(0,2)=BLUE; stage(0,1..3)→OOB → BLACK.
        Stage stage = new Stage(1, 4, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {{ RED }, { GREEN }, { BLUE }}))
                .scrollV(new ParamInt(2))
                .build()
                .render(stage);
        assertEquals(BLUE,  px(stage, 0, 0));
        assertEquals(BLACK, px(stage, 0, 1));
        assertEquals(BLACK, px(stage, 0, 2));
        assertEquals(BLACK, px(stage, 0, 3));
    }

    // ─── scrollH + scrollV combined ───────────────────────────────────────────────

    // ─── zoom() — lifecycle ───────────────────────────────────────────────────────

    @Test
    void zoomReturnsThis() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED));
        assertSame(p, p.zoom(new ParamDouble(1.0)));
    }

    @Test
    void zoomWithNullFactorThrows() {
        assertThrows(NullPointerException.class,
                () -> new EffectPipeline().addSource(solid(1, 1, RED)).zoom(null));
    }

    @Test
    void zoomBeforeAnySourceThrows() {
        assertThrows(IllegalStateException.class,
                () -> new EffectPipeline().zoom(new ParamDouble(1.0)));
    }

    @Test
    void zoomAfterBuildThrows() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED)).build();
        assertThrows(IllegalStateException.class, () -> p.zoom(new ParamDouble(1.0)));
    }

    @Test
    void zoomWithCenterReturnsThis() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED));
        assertSame(p, p.zoom(new ParamDouble(1.0), new ParamDouble(0), new ParamDouble(0)));
    }

    @Test
    void zoomWithCenterNullCxThrows() {
        assertThrows(NullPointerException.class,
                () -> new EffectPipeline().addSource(solid(1, 1, RED))
                        .zoom(new ParamDouble(1.0), null, new ParamDouble(0)));
    }

    @Test
    void zoomWithCenterNullCyThrows() {
        assertThrows(NullPointerException.class,
                () -> new EffectPipeline().addSource(solid(1, 1, RED))
                        .zoom(new ParamDouble(1.0), new ParamDouble(0), null));
    }

    // ─── rotate() — lifecycle ─────────────────────────────────────────────────────

    @Test
    void rotateReturnsThis() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED));
        assertSame(p, p.rotate(new ParamDouble(0)));
    }

    @Test
    void rotateWithNullAngleThrows() {
        assertThrows(NullPointerException.class,
                () -> new EffectPipeline().addSource(solid(1, 1, RED)).rotate(null));
    }

    @Test
    void rotateBeforeAnySourceThrows() {
        assertThrows(IllegalStateException.class,
                () -> new EffectPipeline().rotate(new ParamDouble(0)));
    }

    @Test
    void rotateAfterBuildThrows() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED)).build();
        assertThrows(IllegalStateException.class, () -> p.rotate(new ParamDouble(0)));
    }

    @Test
    void rotateWithCenterReturnsThis() {
        EffectPipeline p = new EffectPipeline().addSource(solid(1, 1, RED));
        assertSame(p, p.rotate(new ParamDouble(0), new ParamDouble(0), new ParamDouble(0)));
    }

    // ─── zoom() — render behaviour ────────────────────────────────────────────────

    @Test
    void renderZoomOneIsIdentity() {
        // factor=1 with explicit centre (0,0): srcX = stageX, srcY = stageY.
        Stage withZoom    = new Stage(3, 1, BLACK);
        Stage withoutZoom = new Stage(3, 1, BLACK);
        ImageSource src = grid(new int[][] {{ RED, GREEN, BLUE }});

        new EffectPipeline().addSource(src)
                .zoom(new ParamDouble(1.0), new ParamDouble(0), new ParamDouble(0))
                .build().render(withZoom);
        new EffectPipeline().addSource(src).build().render(withoutZoom);

        for (int x = 0; x < 3; x++)
            assertEquals(px(withoutZoom, x, 0), px(withZoom, x, 0));
    }

    @Test
    void renderZoomTwoMagnifiesContent() {
        // Source 3×1 [RED, GREEN, BLUE], centre (0,0), factor=2.
        // stage(x) → src(x/2): stage(0)→src(0)=RED, stage(2)→src(1)=GREEN, stage(4)→src(2)=BLUE.
        Stage stage = new Stage(5, 1, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {{ RED, GREEN, BLUE }}))
                .zoom(new ParamDouble(2.0), new ParamDouble(0), new ParamDouble(0))
                .build()
                .render(stage);
        assertEquals(RED,   px(stage, 0, 0));
        assertEquals(GREEN, px(stage, 2, 0));
        assertEquals(BLUE,  px(stage, 4, 0));
    }

    @Test
    void renderZoomHalfDezooms() {
        // Source 5×1 [RED, GREEN, BLUE, RED, GREEN], centre (0,0), factor=0.5.
        // stage(x) → src(x/0.5 = 2x): stage(0)→0=RED, stage(1)→2=BLUE, stage(2)→4=GREEN.
        Stage stage = new Stage(3, 1, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {{ RED, GREEN, BLUE, RED, GREEN }}))
                .zoom(new ParamDouble(0.5), new ParamDouble(0), new ParamDouble(0))
                .build()
                .render(stage);
        assertEquals(RED,   px(stage, 0, 0));
        assertEquals(BLUE,  px(stage, 1, 0));
        assertEquals(GREEN, px(stage, 2, 0));
    }

    @Test
    void renderZoomBoundedOOBShowsBackground() {
        // Source 2×1 [RED, GREEN], centre (0,0), factor=0.5: srcX = stageX * 2.
        // stage(0) → src(0)=RED; stage(1) → src(2) OOB → background BLACK.
        Stage stage = new Stage(2, 1, BLACK);
        new EffectPipeline()
                .addSource(grid(new int[][] {{ RED, GREEN }}))
                .zoom(new ParamDouble(0.5), new ParamDouble(0), new ParamDouble(0))
                .build()
                .render(stage);
        assertEquals(RED,   px(stage, 0, 0));
        assertEquals(BLACK, px(stage, 1, 0));
    }

    @Test
    void renderZoomNonPositiveFactorThrowsAtRender() {
        EffectPipeline p = new EffectPipeline()
                .addSource(solid(1, 1, RED))
                .zoom(new ParamDouble(0))
                .build();
        assertThrows(IllegalArgumentException.class, () -> p.render(new Stage(1, 1)));
    }

    @Test
    void renderZoomDefaultCenterDiffersFromExplicitCenter() {
        // Source 3×1 [RED, GREEN, BLUE], factor=2.
        // Explicit centre (0,0): stage(4) → src(2) = BLUE (in bounds).
        // Default centre (2.5 for width=5): stage(4) → src(2.5 + 0.75) = src(3.25) → OOB → BLACK.
        Stage explicitCenter = new Stage(5, 1, BLACK);
        Stage defaultCenter  = new Stage(5, 1, BLACK);
        ImageSource src = grid(new int[][] {{ RED, GREEN, BLUE }});

        new EffectPipeline().addSource(src)
                .zoom(new ParamDouble(2.0), new ParamDouble(0), new ParamDouble(0))
                .build().render(explicitCenter);
        new EffectPipeline().addSource(src)
                .zoom(new ParamDouble(2.0))
                .build().render(defaultCenter);

        assertEquals(BLUE,  px(explicitCenter, 4, 0));
        assertEquals(BLACK, px(defaultCenter,  4, 0)); // OOB with default centre
    }

    // ─── rotate() — render behaviour ─────────────────────────────────────────────

    @Test
    void renderRotateZeroIsIdentity() {
        Stage withRotate    = new Stage(3, 1, BLACK);
        Stage withoutRotate = new Stage(3, 1, BLACK);
        ImageSource src = grid(new int[][] {{ RED, GREEN, BLUE }});

        new EffectPipeline().addSource(src)
                .rotate(new ParamDouble(0), new ParamDouble(1), new ParamDouble(0))
                .build().render(withRotate);
        new EffectPipeline().addSource(src).build().render(withoutRotate);

        for (int x = 0; x < 3; x++)
            assertEquals(px(withoutRotate, x, 0), px(withRotate, x, 0));
    }

    @Test
    void renderRotateCenterPixelIsAlwaysUnchanged() {
        // For any rotation, the centre pixel (dx=0, dy=0) reads exactly from source centre.
        // Source 3×3 with (1,1)=BLUE, all others=RED.
        Stage stage = new Stage(3, 3, BLACK);
        int[][] grid = {
                { RED, RED,  RED  },
                { RED, BLUE, RED  },
                { RED, RED,  RED  }
        };
        new EffectPipeline()
                .addSource(grid(grid))
                .rotate(new ParamDouble(Math.PI / 4),
                        new ParamDouble(1), new ParamDouble(1))
                .build()
                .render(stage);
        assertEquals(BLUE, px(stage, 1, 1));
    }

    @Test
    void renderRotate180WithWrapFlipsContent() {
        // TileMap 3×1 WRAP [RED, GREEN, BLUE]. Centre explicit (1, 0). Angle = π.
        // srcX = 1 + (x−1)·cos(π) = 2−x  →  stage(0)→src(2)=BLUE, stage(2)→src(0)=RED.
        BufferedImage sheet = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        sheet.setRGB(0, 0, RED);
        sheet.setRGB(1, 0, GREEN);
        sheet.setRGB(2, 0, BLUE);
        TileSet ts = new TileSet(sheet, 1, 1);
        TileMap map = new TileMap(ts, 3, 1, TileMap.EdgePolicy.WRAP);
        map.setTiles(0, 0, new int[][] {{ 0, 1, 2 }});

        Stage stage = new Stage(3, 1, BLACK);
        new EffectPipeline()
                .addSource(map)
                .rotate(new ParamDouble(Math.PI),
                        new ParamDouble(1), new ParamDouble(0))
                .build()
                .render(stage);

        assertEquals(BLUE,  px(stage, 0, 0));
        assertEquals(GREEN, px(stage, 1, 0)); // centre stays
        assertEquals(RED,   px(stage, 2, 0));
    }

    @Test
    void renderRotateUniformSourceProducesUniformResult() {
        // Rotating a uniform source by any angle still fills the stage uniformly
        // (for a WRAP source covering the whole stage).
        BufferedImage sheet = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        sheet.setRGB(0, 0, BLUE);
        TileSet ts = new TileSet(sheet, 1, 1);
        TileMap map = new TileMap(ts, 4, 4, TileMap.EdgePolicy.WRAP);
        map.setTiles(0, 0, new int[][] {{ 0, 0, 0, 0 }, { 0, 0, 0, 0 },
                                        { 0, 0, 0, 0 }, { 0, 0, 0, 0 }});

        Stage stage = new Stage(4, 4, BLACK);
        new EffectPipeline()
                .addSource(map)
                .rotate(new ParamDouble(Math.PI / 3))
                .build()
                .render(stage);

        for (int y = 0; y < 4; y++)
            for (int x = 0; x < 4; x++)
                assertEquals(BLUE, px(stage, x, y),
                        "Expected BLUE at (" + x + ", " + y + ")");
    }

    // ─── scrollH + scrollV combined ───────────────────────────────────────────────

    @Test
    void renderScrollHAndScrollVCombine() {
        // Source 3×3. scrollH(1).scrollV(1): stage(x,y) reads src(x+1, y+1).
        Stage stage = new Stage(2, 2, MAGENTA);
        new EffectPipeline()
                .addSource(grid(new int[][] {
                        { RED,   GREEN, BLUE  },
                        { BLUE,  RED,   GREEN },
                        { GREEN, BLUE,  RED   }
                }))
                .scrollH(new ParamInt(1))
                .scrollV(new ParamInt(1))
                .build()
                .render(stage);
        assertEquals(RED,   px(stage, 0, 0)); // src(1,1)
        assertEquals(GREEN, px(stage, 1, 0)); // src(2,1)
        assertEquals(BLUE,  px(stage, 0, 1)); // src(1,2)
        assertEquals(RED,   px(stage, 1, 1)); // src(2,2)
    }
}