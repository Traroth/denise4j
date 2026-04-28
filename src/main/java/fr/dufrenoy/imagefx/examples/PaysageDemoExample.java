/*
 * PaysageDemoExample.java
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
import fr.dufrenoy.imagefx.staging.EffectPipeline;
import fr.dufrenoy.imagefx.staging.ParamInt;
import fr.dufrenoy.imagefx.staging.Stage;

import javax.imageio.ImageIO;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Fullscreen demo: {@code paysage_montage.jpg} with multidirectional
 * Lissajous scrolling.
 *
 * <p>The viewport follows a compound Lissajous curve (3:2 frequency ratio
 * plus a secondary harmonic), producing a fast, rhythmic trajectory that
 * traces figure-eight patterns across the landscape without repeating
 * quickly. The image must be larger than the screen for panning to be
 * visible; pixels outside its bounds show the black background.</p>
 *
 * <p>Press {@code SPACE} to quit.</p>
 */
public class PaysageDemoExample {

    // ─── Animation constants ─────────────────────────────────────────────────────

    /** Target frame rate in frames per second. */
    private static final int TARGET_FPS = 120;

    /**
     * Scroll speed in radians per second. Controls the pace of the Lissajous
     * trajectory independently of the frame rate. {@code 0.54} rad/s gives
     * roughly one Lissajous period in ~12 s.
     */
    private static final double SCROLL_SPEED_RAD_PER_S = 0.54;

    /** Weight of the primary Lissajous harmonic. */
    private static final double PRIMARY_WEIGHT = 0.75;

    /** Weight of the secondary harmonic (adds rhythmic jitter). */
    private static final double SECONDARY_WEIGHT = 0.25;

    // ─── Entry point ─────────────────────────────────────────────────────────────

    /**
     * Launches the demo.
     *
     * @param args unused
     * @throws IOException if {@code paysage_montage.jpg} cannot be loaded
     */
    public static void main(String[] args) throws IOException {
        BufferedImage image;
        try (InputStream is = PaysageDemoExample.class.getClassLoader()
                .getResourceAsStream("paysage_montage.jpg")) {
            if (is == null) {
                throw new IOException("paysage_montage.jpg not found in classpath resources");
            }
            image = ImageIO.read(is);
        }

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = screen.width;
        int h = screen.height;

        // Scroll ranges — clamped to 1 so ampH/ampV are never zero even if the
        // image is smaller than the screen (visible panning requires imgW > w).
        int maxH = Math.max(1, image.getWidth()  - w);
        int maxV = Math.max(1, image.getHeight() - h);
        int centerH = maxH / 2;
        int centerV = maxV / 2;
        int ampH = maxH / 2;
        int ampV = maxV / 2;

        ImageSource source = new ImageSource(image);

        ParamInt scrollH = new ParamInt(centerH);
        ParamInt scrollV = new ParamInt(centerV);

        EffectPipeline pipeline = new EffectPipeline()
                .addSource(source)
                    .scrollH(scrollH)
                    .scrollV(scrollV)
                .build();

        StagePool pool = new StagePool(w, h, 3, FrameDropPolicy.REPEAT_LAST);

        double[] elapsedS = {0.0};

        Orchestrator orchestrator = new Orchestrator()
                .pipeline(pipeline)
                .stagePool(pool)
                .targetFps(TARGET_FPS)
                .onFrame((frameIndex, deltaMs) -> {
                    elapsedS[0] += deltaMs / 1000.0;
                    double t = elapsedS[0] * SCROLL_SPEED_RAD_PER_S;
                    // Lissajous 3:2 — secondary harmonic at 7:5 adds rhythmic feel
                    scrollH.set(centerH + (int)(ampH * (PRIMARY_WEIGHT   * Math.sin(3 * t)
                                                       + SECONDARY_WEIGHT * Math.sin(7 * t))));
                    scrollV.set(centerV + (int)(ampV * (PRIMARY_WEIGHT   * Math.sin(2 * t)
                                                       + SECONDARY_WEIGHT * Math.sin(5 * t))));
                })
                .build();

        orchestrator.start();

        // ─── AWT window ──────────────────────────────────────────────────────────

        Canvas canvas = new Canvas();
        canvas.setBackground(Color.BLACK);
        canvas.setFocusable(true);

        Frame frame = new Frame("Paysage — denise4j demo");
        frame.setUndecorated(true);
        frame.add(canvas);

        GraphicsDevice gd = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();
        gd.setFullScreenWindow(frame);

        // ─── Display loop (active rendering) ────────────────────────────────────

        Timer displayTimer = new Timer("denise4j-display", true);
        displayTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Graphics g = canvas.getGraphics();
                if (g == null) return;
                try {
                    Stage front = orchestrator.getFrontBuffer();
                    if (front != null) {
                        g.drawImage(front.getImage(), 0, 0, null);
                    }
                } finally {
                    g.dispose();
                }
            }
        }, 0L, 1000L / TARGET_FPS);

        // ─── Keyboard — SPACE to quit ────────────────────────────────────────────

        KeyAdapter quitOnSpace = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_SPACE) {
                    displayTimer.cancel();
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
}