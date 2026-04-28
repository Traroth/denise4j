/*
 * FleursDemoExample.java
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
import fr.dufrenoy.imagefx.staging.ParamDouble;
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
 * Fullscreen demo: {@code fleurs.jpg} animated with a rotating zoom effect.
 *
 * <p>Combines a slow clockwise rotation with a sinusoidal zoom (breathing
 * effect) to produce a mandala-like animation. The zoom oscillates between
 * {@code ZOOM_CENTER - ZOOM_AMPLITUDE} and {@code ZOOM_CENTER + ZOOM_AMPLITUDE}.
 * Press {@code SPACE} to quit.</p>
 *
 * <p>Uses only AWT — no Swing dependency.</p>
 */
public class FleursDemoExample {

    // ─── Animation constants ─────────────────────────────────────────────────────

    /** Full rotation period in seconds. */
    private static final double ROTATION_PERIOD_S = 40.0;

    /** Zoom oscillation period in seconds. */
    private static final double ZOOM_PERIOD_S = 7.0;

    /** Midpoint zoom factor. */
    private static final double ZOOM_CENTER = 1.5;

    /** Half-amplitude of the zoom oscillation. */
    private static final double ZOOM_AMPLITUDE = 0.8;

    /** Target frame rate in frames per second. */
    private static final int TARGET_FPS = 60;

    // ─── Entry point ─────────────────────────────────────────────────────────────

    /**
     * Launches the demo.
     *
     * @param args unused
     * @throws IOException if {@code fleurs.jpg} cannot be loaded from resources
     */
    public static void main(String[] args) throws IOException {
        BufferedImage image;
        try (InputStream is = FleursDemoExample.class.getClassLoader()
                .getResourceAsStream("fleurs.jpg")) {
            if (is == null) {
                throw new IOException("fleurs.jpg not found in classpath resources");
            }
            image = ImageIO.read(is);
        }

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = screen.width;
        int h = screen.height;

        ImageSource source = new ImageSource(image);

        ParamDouble angle = new ParamDouble(0.0);
        ParamDouble zoom  = new ParamDouble(ZOOM_CENTER);

        EffectPipeline pipeline = new EffectPipeline()
                .addSource(source)
                    .rotate(angle)
                    .zoom(zoom)
                .build();

        StagePool pool = new StagePool(w, h, 3, FrameDropPolicy.REPEAT_LAST);

        Orchestrator orchestrator = new Orchestrator()
                .pipeline(pipeline)
                .stagePool(pool)
                .targetFps(TARGET_FPS)
                .onFrame((frameIndex, deltaMs) -> {
                    // Slow clockwise rotation — one full turn every ROTATION_PERIOD_S seconds
                    angle.set(frameIndex * (2.0 * Math.PI / (TARGET_FPS * ROTATION_PERIOD_S)));
                    // Sinusoidal zoom — breathes between (CENTER - AMP) and (CENTER + AMP)
                    double zoomPhase = frameIndex * (2.0 * Math.PI / (TARGET_FPS * ZOOM_PERIOD_S));
                    zoom.set(ZOOM_CENTER + ZOOM_AMPLITUDE * Math.sin(zoomPhase));
                })
                .build();

        orchestrator.start();

        // ─── AWT window ──────────────────────────────────────────────────────────

        Canvas canvas = new Canvas();
        canvas.setBackground(Color.BLACK);
        canvas.setFocusable(true);

        Frame frame = new Frame("Fleurs — denise4j demo");
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