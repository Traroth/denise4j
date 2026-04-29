/*
 * ShadowDemo.java
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
package fr.dufrenoy.imagefx.examples;

import fr.dufrenoy.imagefx.orchestration.FrameDropPolicy;
import fr.dufrenoy.imagefx.orchestration.Orchestrator;
import fr.dufrenoy.imagefx.orchestration.StagePool;
import fr.dufrenoy.imagefx.source.ImageSource;
import fr.dufrenoy.imagefx.source.WrappingImageSource;
import fr.dufrenoy.imagefx.staging.EffectPipeline;
import fr.dufrenoy.imagefx.staging.ParamInt;
import fr.dufrenoy.imagefx.staging.Stage;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.BufferStrategy;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fullscreen parallax scrolling demo inspired by <em>Shadow of the Beast</em>
 * (Psygnosis, Amiga, 1989).
 *
 * <p>Seven scrolling layers compose the scene, from back to front:</p>
 * <ol>
 *   <li>Sky — steel-blue-to-coral gradient with pale moon and clouds,
 *       baked into a static {@link ImageSource} (no scroll)</li>
 *   <li>Distant hills (2 %) — flat horizon silhouette, cool blue-gray</li>
 *   <li>Far mountains (5 %) — triangular peaks, dusty warm gray</li>
 *   <li>Far rock spires (9 %) — tall ochre aiguilles</li>
 *   <li>Mid rocks (14 %) — medium ochre formations</li>
 *   <li>Near rocky masses (22 %) — broad dark-olive formations</li>
 *   <li>Near foreground (36 %) — low dark-green masses at ground level</li>
 *   <li>Dark standing stones (55 %) — narrow near-black green menhirs</li>
 * </ol>
 *
 * <p>Scrolling layers use {@link WrappingImageSource}, which tiles the
 * source image toroidally with direct array access (no TileMap overhead).
 * Display is driven by {@link BufferStrategy} page flipping, which
 * synchronises to the monitor's vertical blank to eliminate tearing.
 * Press {@code SPACE} to quit. No Swing dependency.</p>
 */
public class ShadowDemo {

    // ─── Timing ──────────────────────────────────────────────────────────────────

    private static final int    TARGET_FPS      = 60;
    private static final double SCROLL_PX_PER_S = 220.0;

    // ─── Compositing color key ───────────────────────────────────────────────────

    private static final int   TRANSPARENT = 0xFFFF00FF;
    private static final Color C_KEY       = new Color(0xFF00FF, false);

    // ─── Palette — matched to Shadow of the Beast screenshots ────────────────────

    private static final Color C_SKY_TOP    = new Color(0x4A6888, false); // steel blue
    private static final Color C_SKY_BOTTOM = new Color(0xE06050, false); // warm coral
    private static final Color C_MOON       = new Color(0xD0CEC0, false); // pale cream
    private static final Color C_CLOUD_LT   = new Color(0xECECE8, false); // near white
    private static final Color C_CLOUD_SHD  = new Color(0xBCBCB4, false); // cloud shadow
    private static final Color C_HILLS      = new Color(0x607080, false); // horizon haze
    private static final Color C_MOUNTAIN   = new Color(0x7A6858, false); // dusty warm gray
    private static final Color C_ROCK_FAR   = new Color(0xBCA060, false); // sandy/ochre
    private static final Color C_ROCK_MID   = new Color(0x9A8050, false); // medium ochre
    private static final Color C_ROCK_NEAR  = new Color(0x507030, false); // dark olive green
    private static final Color C_FG_NEAR    = new Color(0x285020, false); // dark forest green
    private static final Color C_SLATE      = new Color(0x142808, false); // near-black green
    private static final Color C_GROUND     = new Color(0x60A010, false); // bright lime green

    // ─── Entry point ─────────────────────────────────────────────────────────────

    /**
     * Launches the demo.
     *
     * @param args unused
     */
    public static void main(String[] args) throws InterruptedException {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = screen.width;
        int h = screen.height;

        // ─── Build layers ─────────────────────────────────────────────────────

        ImageSource          skySource  = new ImageSource(buildSky(w, h));
        WrappingImageSource  hillsSrc   = new WrappingImageSource(buildDistantHills(w, h));
        WrappingImageSource  mountSrc   = new WrappingImageSource(buildFarMountains(w, h));
        WrappingImageSource  farSrc     = new WrappingImageSource(buildFarRocks(w, h));
        WrappingImageSource  midSrc     = new WrappingImageSource(buildMidRocks(w, h));
        WrappingImageSource  nearSrc    = new WrappingImageSource(buildNearRocks(w, h));
        WrappingImageSource  fgNearSrc  = new WrappingImageSource(buildNearForeground(w, h));
        WrappingImageSource  slateSrc   = new WrappingImageSource(buildForegroundStones(w, h));

        // ─── Scroll parameters ────────────────────────────────────────────────

        ParamInt hillsScroll  = new ParamInt(0);
        ParamInt mountScroll  = new ParamInt(0);
        ParamInt farScroll    = new ParamInt(0);
        ParamInt midScroll    = new ParamInt(0);
        ParamInt nearScroll   = new ParamInt(0);
        ParamInt fgNearScroll = new ParamInt(0);
        ParamInt slateScroll  = new ParamInt(0);

        // ─── Pipeline (back to front) ─────────────────────────────────────────

        EffectPipeline pipeline = new EffectPipeline()
                .addSource(skySource)
                .addSource(hillsSrc) .transparentColor(TRANSPARENT).scrollH(hillsScroll)
                .addSource(mountSrc) .transparentColor(TRANSPARENT).scrollH(mountScroll)
                .addSource(farSrc)   .transparentColor(TRANSPARENT).scrollH(farScroll)
                .addSource(midSrc)   .transparentColor(TRANSPARENT).scrollH(midScroll)
                .addSource(nearSrc)  .transparentColor(TRANSPARENT).scrollH(nearScroll)
                .addSource(fgNearSrc).transparentColor(TRANSPARENT).scrollH(fgNearScroll)
                .addSource(slateSrc) .transparentColor(TRANSPARENT).scrollH(slateScroll)
                .build();

        StagePool pool    = new StagePool(w, h, 3, FrameDropPolicy.REPEAT_LAST);
        double[] elapsedS = {0.0};

        Orchestrator orchestrator = new Orchestrator()
                .pipeline(pipeline)
                .stagePool(pool)
                .targetFps(TARGET_FPS)
                .onFrame((frameIndex, deltaMs) -> {
                    elapsedS[0] += deltaMs / 1000.0;
                    double px = elapsedS[0] * SCROLL_PX_PER_S;
                    hillsScroll .set((int)(px * 0.02));
                    mountScroll .set((int)(px * 0.05));
                    farScroll   .set((int)(px * 0.09));
                    midScroll   .set((int)(px * 0.14));
                    nearScroll  .set((int)(px * 0.22));
                    fgNearScroll.set((int)(px * 0.36));
                    slateScroll .set((int)(px * 0.55));
                })
                .build();

        orchestrator.start();

        // ─── AWT window ──────────────────────────────────────────────────────

        Canvas canvas = new Canvas();
        canvas.setBackground(Color.BLACK);
        canvas.setFocusable(true);

        Frame frame = new Frame("Shadow — denise4j demo");
        frame.setUndecorated(true);
        frame.add(canvas);

        GraphicsDevice gd = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();
        gd.setFullScreenWindow(frame);

        // Give the native window system time to commit the fullscreen request
        // before creating the BufferStrategy.
        Thread.sleep(50);
        canvas.createBufferStrategy(2);
        BufferStrategy bs = canvas.getBufferStrategy();

        // ─── Display thread ───────────────────────────────────────────────────
        // BufferStrategy.show() performs a page flip synchronised to the
        // monitor's vertical blank in fullscreen exclusive mode, eliminating
        // tearing. Toolkit.sync() flushes the X11 pipeline on Linux.

        AtomicBoolean displayRunning = new AtomicBoolean(true);
        Thread displayThread = new Thread(() -> {
            while (displayRunning.get()) {
                Stage front = orchestrator.getFrontBuffer();
                do {
                    Graphics g = bs.getDrawGraphics();
                    try {
                        g.drawImage(front.getImage(), 0, 0, null);
                    } finally {
                        g.dispose();
                    }
                } while (bs.contentsLost());
                bs.show();
                Toolkit.getDefaultToolkit().sync();
            }
        }, "denise4j-display");
        displayThread.setDaemon(true);
        displayThread.start();

        // ─── Keyboard — SPACE to quit ─────────────────────────────────────────

        KeyAdapter quitOnSpace = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_SPACE) {
                    displayRunning.set(false);
                    new Thread(() -> {
                        orchestrator.stop();
                        gd.setFullScreenWindow(null);
                        frame.dispose();
                        System.exit(0);
                    }, "denise4j-shutdown").start();
                }
            }
        };
        frame.addKeyListener(quitOnSpace);
        canvas.addKeyListener(quitOnSpace);
        canvas.requestFocus();
    }

    // ─── Layer generation ────────────────────────────────────────────────────────

    /**
     * Full-screen static layer: sky gradient, large moon, baked clouds,
     * and bright-green ground strip.
     */
    private static BufferedImage buildSky(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        int groundY = (int)(h * 0.87);

        g.setPaint(new GradientPaint(0, 0, C_SKY_TOP, 0, groundY, C_SKY_BOTTOM));
        g.fillRect(0, 0, w, groundY);

        g.setColor(C_GROUND);
        g.fillRect(0, groundY, w, h - groundY);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int moonR  = (int)(h * 0.115);
        int moonCx = (int)(w * 0.82);
        int moonCy = (int)(h * 0.17);
        g.setColor(C_MOON);
        g.fillOval(moonCx - moonR, moonCy - moonR, moonR * 2, moonR * 2);

        double[][] clouds = {
            {0.13, 0.11, 0.19, 0.085},
            {0.38, 0.07, 0.24, 0.100},
            {0.62, 0.13, 0.16, 0.070},
            {0.84, 0.09, 0.15, 0.080},
        };
        for (double[] c : clouds) {
            drawCloud(g, (int)(c[0]*w), (int)(c[1]*h), (int)(c[2]*w), (int)(c[3]*h));
        }

        g.dispose();
        return img;
    }

    /**
     * Very flat horizon silhouette — cool blue-gray, base at 84 % height.
     * Barely-moving wide mesas create the illusion of a far landscape.
     */
    private static BufferedImage buildDistantHills(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        noAA(g);
        fillKey(g, w, h);
        g.setColor(C_HILLS);

        double[][] hills = {
            {0.08, 0.74, 0.095, 0.105, 1},
            {0.24, 0.73, 0.115, 0.100, 1},
            {0.40, 0.75, 0.090, 0.110, 1},
            {0.57, 0.72, 0.125, 0.105, 1},
            {0.73, 0.74, 0.100, 0.095, 1},
            {0.89, 0.75, 0.105, 0.090, 1},
        };
        drawSpireLayer(g, w, h, (int)(h * 0.84), hills);
        g.dispose();
        return img;
    }

    /**
     * Triangular mountain peaks — dusty warm gray, base at 79 % height.
     */
    private static BufferedImage buildFarMountains(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        noAA(g);
        fillKey(g, w, h);
        g.setColor(C_MOUNTAIN);

        double[][] mountains = {
            {0.05, 0.63, 0.055, 0.065, 0},
            {0.17, 0.66, 0.075, 0.060, 0},
            {0.29, 0.61, 0.065, 0.070, 0},
            {0.42, 0.64, 0.070, 0.065, 0},
            {0.54, 0.62, 0.060, 0.075, 0},
            {0.66, 0.65, 0.075, 0.065, 0},
            {0.78, 0.63, 0.065, 0.060, 0},
            {0.90, 0.65, 0.070, 0.070, 0},
        };
        drawSpireLayer(g, w, h, (int)(h * 0.79), mountains);
        g.dispose();
        return img;
    }

    /**
     * Far sandy rock spires — tall triangular ochre peaks, some mesa-capped,
     * base at 67 % height.
     */
    private static BufferedImage buildFarRocks(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        noAA(g);
        fillKey(g, w, h);
        g.setColor(C_ROCK_FAR);

        double[][] spires = {
            {0.04, 0.31, 0.025, 0.035, 0},
            {0.10, 0.24, 0.038, 0.028, 0},
            {0.17, 0.33, 0.020, 0.042, 0},
            {0.25, 0.27, 0.048, 0.032, 0},
            {0.33, 0.38, 0.060, 0.055, 1},
            {0.42, 0.28, 0.030, 0.025, 0},
            {0.49, 0.22, 0.040, 0.035, 0},
            {0.57, 0.30, 0.028, 0.048, 0},
            {0.65, 0.35, 0.055, 0.045, 1},
            {0.73, 0.25, 0.032, 0.028, 0},
            {0.80, 0.29, 0.038, 0.042, 0},
            {0.87, 0.21, 0.028, 0.034, 0},
            {0.94, 0.34, 0.022, 0.030, 0},
        };
        drawSpireLayer(g, w, h, (int)(h * 0.67), spires);
        g.dispose();
        return img;
    }

    /**
     * Mid-distance rock formations — medium ochre, base at 71 % height.
     * Fills the visual gap between the far spires and the near masses.
     */
    private static BufferedImage buildMidRocks(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        noAA(g);
        fillKey(g, w, h);
        g.setColor(C_ROCK_MID);

        double[][] spires = {
            {0.05, 0.36, 0.030, 0.026, 0},
            {0.13, 0.40, 0.048, 0.040, 1},
            {0.22, 0.33, 0.028, 0.038, 0},
            {0.31, 0.38, 0.052, 0.044, 0},
            {0.40, 0.35, 0.034, 0.030, 0},
            {0.49, 0.42, 0.058, 0.050, 1},
            {0.58, 0.37, 0.040, 0.032, 0},
            {0.67, 0.33, 0.030, 0.042, 0},
            {0.76, 0.40, 0.048, 0.038, 0},
            {0.85, 0.36, 0.032, 0.028, 0},
            {0.93, 0.39, 0.044, 0.040, 0},
        };
        drawSpireLayer(g, w, h, (int)(h * 0.71), spires);
        g.dispose();
        return img;
    }

    /**
     * Near rocky masses — broader dark-olive formations, base at 76 % height.
     */
    private static BufferedImage buildNearRocks(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        noAA(g);
        fillKey(g, w, h);
        g.setColor(C_ROCK_NEAR);

        double[][] spires = {
            {0.06, 0.42, 0.055, 0.050, 0},
            {0.17, 0.46, 0.070, 0.065, 1},
            {0.29, 0.39, 0.048, 0.058, 0},
            {0.41, 0.44, 0.060, 0.052, 0},
            {0.53, 0.40, 0.072, 0.060, 1},
            {0.64, 0.47, 0.052, 0.048, 0},
            {0.75, 0.41, 0.065, 0.055, 0},
            {0.86, 0.45, 0.048, 0.060, 0},
            {0.95, 0.43, 0.040, 0.038, 0},
        };
        drawSpireLayer(g, w, h, (int)(h * 0.76), spires);
        g.dispose();
        return img;
    }

    /**
     * Near foreground masses — low dark-green formations just above the ground,
     * base at 83 % height.
     */
    private static BufferedImage buildNearForeground(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        noAA(g);
        fillKey(g, w, h);
        g.setColor(C_FG_NEAR);

        double[][] masses = {
            {0.07, 0.57, 0.082, 0.075, 1},
            {0.21, 0.60, 0.095, 0.088, 1},
            {0.36, 0.56, 0.078, 0.085, 1},
            {0.51, 0.59, 0.090, 0.080, 1},
            {0.65, 0.57, 0.082, 0.090, 1},
            {0.79, 0.58, 0.092, 0.078, 1},
            {0.93, 0.60, 0.075, 0.082, 1},
        };
        drawSpireLayer(g, w, h, (int)(h * 0.83), masses);
        g.dispose();
        return img;
    }

    /**
     * Dark foreground standing stones — narrow near-black green menhir silhouettes,
     * bases at the ground line (87 % height).
     */
    private static BufferedImage buildForegroundStones(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        noAA(g);
        fillKey(g, w, h);
        g.setColor(C_SLATE);

        int groundY = (int)(h * 0.87);
        double[][] stones = {
            {0.05, 0.46, 0.024, 0.020},
            {0.14, 0.52, 0.018, 0.022},
            {0.25, 0.44, 0.028, 0.026},
            {0.36, 0.49, 0.020, 0.024},
            {0.48, 0.42, 0.032, 0.028},
            {0.59, 0.47, 0.022, 0.018},
            {0.70, 0.44, 0.026, 0.030},
            {0.81, 0.50, 0.020, 0.022},
            {0.91, 0.46, 0.028, 0.024},
        };
        for (double[] s : stones) {
            int cx     = (int)(s[0] * w);
            int topY   = (int)(s[1] * h);
            int leftW  = (int)(s[2] * w);
            int rightW = (int)(s[3] * w);
            // Slight taper: 2 px wider at base on each side
            int[] xs = {cx - leftW, cx + rightW, cx + rightW + 2, cx - leftW - 2};
            int[] ys = {topY,       topY,         groundY,         groundY        };
            g.fillPolygon(xs, ys, 4);
        }
        g.dispose();
        return img;
    }

    // ─── Drawing helpers ─────────────────────────────────────────────────────────

    private static void noAA(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    private static void fillKey(Graphics2D g, int w, int h) {
        g.setColor(C_KEY);
        g.fillRect(0, 0, w, h);
    }

    /**
     * Builds a silhouette profile and fills from that profile to the bottom of
     * the tile. Spire spec: {@code {cx ratio, peak-y ratio, left-half-width ratio,
     * right-half-width ratio, shape}} where shape 0 = triangular, 1 = mesa.
     */
    private static void drawSpireLayer(Graphics2D g, int w, int h, int baseY, double[][] spires) {
        int[] profile = new int[w];
        for (int x = 0; x < w; x++) profile[x] = baseY;

        for (double[] s : spires) {
            int cx    = (int)(s[0] * w);
            int peakY = (int)(s[1] * h);
            int leftW = (int)(s[2] * w);
            int rightW= (int)(s[3] * w);
            int shape = (int) s[4];

            for (int x = Math.max(0, cx - leftW); x < Math.min(w, cx + rightW); x++) {
                int halfW = (x < cx) ? leftW : rightW;
                if (halfW == 0) continue;
                int dist = Math.abs(x - cx);
                int y;
                if (shape == 1) {
                    int flatW = halfW / 4;
                    if (dist <= flatW) {
                        y = peakY;
                    } else {
                        double t = (double)(dist - flatW) / (halfW - flatW);
                        y = (int)(peakY + (baseY - peakY) * t);
                    }
                } else {
                    double t = (double) dist / halfW;
                    y = (int)(peakY + (baseY - peakY) * t);
                }
                profile[x] = Math.min(profile[x], y);
            }
        }

        int[] xs = new int[w + 2];
        int[] ys = new int[w + 2];
        for (int x = 0; x < w; x++) { xs[x] = x; ys[x] = profile[x]; }
        xs[w] = w - 1; ys[w] = h;
        xs[w + 1] = 0; ys[w + 1] = h;
        g.fillPolygon(xs, ys, w + 2);
    }

    /**
     * Draws a puffy cloud with smooth antialiased edges.
     * Shadow layer first, bright lobes on top.
     */
    private static void drawCloud(Graphics2D g, int cx, int cy, int cw, int ch) {
        g.setColor(C_CLOUD_SHD);
        g.fillOval(cx - cw * 2/5, cy,           cw * 4/5, ch / 2);
        g.fillOval(cx - cw / 2,   cy - ch / 4,  cw * 2/5, ch * 3/4);
        g.fillOval(cx + cw / 10,  cy - ch / 4,  cw * 2/5, ch * 3/4);

        g.setColor(C_CLOUD_LT);
        g.fillOval(cx - cw * 3/8, cy - ch,      cw * 3/4, ch);
        g.fillOval(cx - cw / 2,   cy - ch * 3/4,cw * 2/5, ch * 3/4);
        g.fillOval(cx + cw / 8,   cy - ch * 7/8,cw * 3/8, ch * 7/8);
        g.fillOval(cx - cw / 8,   cy - ch * 5/4,cw / 3,   ch * 5/8);
    }
}