/*
 * Orchestrator.java
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
package fr.dufrenoy.imagefx.orchestration;

import fr.dufrenoy.imagefx.staging.EffectPipeline;
import fr.dufrenoy.imagefx.staging.Stage;

/**
 * High-level animation loop that drives an {@link EffectPipeline} at a target
 * frame rate.
 *
 * <p>The orchestrator runs a dedicated daemon render thread. Each iteration:</p>
 * <ol>
 *   <li>Acquires a free back buffer from the {@link StagePool} (blocking if
 *       none is available).</li>
 *   <li>Invokes the {@link FrameCallback} so the caller can update its
 *       {@link ParamInt} / {@link ParamDouble} values.</li>
 *   <li>Calls {@link EffectPipeline#render(Stage)} on the back buffer.</li>
 *   <li>Presents the back buffer to the pool.</li>
 *   <li>Sleeps any remaining time to honour the target frame rate.</li>
 * </ol>
 *
 * <p>The display side is left to the caller. From the display thread (a Swing
 * {@code Timer}, a JavaFX {@code AnimationTimer}, etc.) call
 * {@link #getFrontBuffer()} to obtain the latest completed frame.</p>
 *
 * <p>Use the fluent configuration methods then {@link #build()} before calling
 * {@link #start()}.</p>
 *
 * <pre>{@code
 * StagePool pool = new StagePool(800, 600, 2, FrameDropPolicy.REPEAT_LAST);
 *
 * Orchestrator orchestrator = new Orchestrator()
 *     .pipeline(pipeline)
 *     .stagePool(pool)
 *     .targetFps(60)
 *     .onFrame((frameIndex, deltaMs) -> {
 *         scrollOffset.add(2);
 *     })
 *     .build();
 *
 * orchestrator.start();
 *
 * // In Swing Timer callback:
 * g.drawImage(orchestrator.getFrontBuffer().getImage(), 0, 0, null);
 *
 * orchestrator.stop();
 * }</pre>
 */
public class Orchestrator {

    /*@
      @ public invariant running ==> built;
      @ public invariant built ==> pipeline != null;
      @ public invariant built ==> stagePool != null;
      @ public invariant built ==> targetFps > 0;
      @ public invariant built ==> callback != null;
      @*/

    // ─── Configuration fields (set before build()) ───────────────────────────────

    private EffectPipeline pipeline;
    private StagePool stagePool;
    private int targetFps;
    private FrameCallback callback;

    // ─── Runtime fields (valid after build()) ────────────────────────────────────

    private boolean built = false;
    private volatile boolean running = false;
    private Thread renderThread;

    // ─── Constructors ────────────────────────────────────────────────────────────

    /**
     * Creates a new, unconfigured orchestrator.
     *
     * <p>Call the fluent configuration methods then {@link #build()} before
     * calling {@link #start()}.</p>
     */
    public Orchestrator() {
    }

    // ─── Fluent configuration ────────────────────────────────────────────────────

    /**
     * Sets the effect pipeline to render each frame.
     *
     * @param pipeline a built pipeline; must not be {@code null}
     * @return this orchestrator
     * @throws IllegalStateException if {@link #build()} has already been called
     */
    //@ requires !built;
    //@ ensures this.pipeline == pipeline;
    //@ ensures \result == this;
    public Orchestrator pipeline(EffectPipeline pipeline) {
        checkNotBuilt();
        this.pipeline = pipeline;
        return this;
    }

    /**
     * Sets the stage pool that provides the back and front buffers.
     *
     * @param stagePool the pool to use; must not be {@code null}
     * @return this orchestrator
     * @throws IllegalStateException if {@link #build()} has already been called
     */
    //@ requires !built;
    //@ ensures this.stagePool == stagePool;
    //@ ensures \result == this;
    public Orchestrator stagePool(StagePool stagePool) {
        checkNotBuilt();
        this.stagePool = stagePool;
        return this;
    }

    /**
     * Sets the target frame rate for the render thread.
     *
     * @param fps the desired number of frames per second; must be strictly
     *            positive
     * @return this orchestrator
     * @throws IllegalStateException if {@link #build()} has already been called
     */
    //@ requires !built;
    //@ ensures this.targetFps == fps;
    //@ ensures \result == this;
    public Orchestrator targetFps(int fps) {
        checkNotBuilt();
        this.targetFps = fps;
        return this;
    }

    /**
     * Sets the callback invoked once per frame, before rendering, on the
     * render thread.
     *
     * @param callback the frame callback; must not be {@code null}
     * @return this orchestrator
     * @throws IllegalStateException if {@link #build()} has already been called
     */
    //@ requires !built;
    //@ ensures this.callback == callback;
    //@ ensures \result == this;
    public Orchestrator onFrame(FrameCallback callback) {
        checkNotBuilt();
        this.callback = callback;
        return this;
    }

    /**
     * Validates the configuration and locks this orchestrator for use.
     *
     * @return this orchestrator
     * @throws IllegalStateException if any required field is missing or invalid
     */
    //@ requires !built;
    //@ requires pipeline != null;
    //@ requires stagePool != null;
    //@ requires targetFps > 0;
    //@ requires callback != null;
    //@ ensures built;
    //@ ensures \result == this;
    public Orchestrator build() {
        checkNotBuilt();
        if (pipeline == null) {
            throw new IllegalStateException("pipeline must be set before build()");
        }
        if (stagePool == null) {
            throw new IllegalStateException("stagePool must be set before build()");
        }
        if (targetFps <= 0) {
            throw new IllegalStateException("targetFps must be strictly positive");
        }
        if (callback == null) {
            throw new IllegalStateException("onFrame callback must be set before build()");
        }
        built = true;
        return this;
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    /**
     * Starts the render thread.
     *
     * @throws IllegalStateException if {@link #build()} has not been called, or
     *         if the orchestrator is already running
     */
    //@ requires built;
    //@ requires !running;
    //@ ensures running;
    //@ ensures renderThread != null;
    //@ ensures renderThread.isDaemon();
    public void start() {
        if (!built) {
            throw new IllegalStateException("build() must be called before start()");
        }
        if (running) {
            throw new IllegalStateException("Orchestrator is already running");
        }
        running = true;
        renderThread = new Thread(this::renderLoop, "denise4j-render");
        renderThread.setDaemon(true);
        renderThread.start();
    }

    /**
     * Signals the render thread to stop and waits for it to terminate.
     *
     * <p>Blocks until the render thread has exited. Safe to call from any
     * thread.</p>
     *
     * @throws IllegalStateException if the orchestrator is not running
     */
    //@ requires running;
    //@ ensures !running;
    public void stop() {
        if (!running) {
            throw new IllegalStateException("Orchestrator is not running");
        }
        running = false;
        renderThread.interrupt();
        try {
            renderThread.join();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ─── Display-thread API ──────────────────────────────────────────────────────

    /**
     * Advances to the next completed frame and returns the front buffer.
     *
     * <p>Delegates to {@link StagePool#getFrontBuffer()}. The behaviour when
     * no frame is ready is determined by the pool's {@link FrameDropPolicy}.</p>
     *
     * <p>Must be called from the display thread only.</p>
     *
     * @return the front buffer, or {@code null} if the policy is
     *         {@link FrameDropPolicy#SKIP} and no frame is ready
     */
    //@ requires built;
    public Stage getFrontBuffer() {
        if (!built) {
            throw new IllegalStateException("build() must be called before getFrontBuffer()");
        }
        return stagePool.getFrontBuffer();
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    private void renderLoop() {
        final long frameNanos = 1_000_000_000L / targetFps;
        long frameIndex = 0;
        long lastFrameStart = -1L;
        // Absolute target for the end of the current frame; corrects accumulated drift.
        long targetTime = System.nanoTime() + frameNanos;

        while (running) {
            long frameStart = System.nanoTime();
            long deltaMs = lastFrameStart < 0 ? 0L : (frameStart - lastFrameStart) / 1_000_000L;
            lastFrameStart = frameStart;

            try {
                Stage back = stagePool.acquireBackBuffer();
                callback.onFrame(frameIndex, deltaMs);
                pipeline.render(back);
                stagePool.present(back);
                frameIndex++;

                long remaining = targetTime - System.nanoTime();
                if (remaining > 1_000_000L) {
                    Thread.sleep(remaining / 1_000_000L, (int)(remaining % 1_000_000L));
                }
                targetTime += frameNanos;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void checkNotBuilt() {
        if (built) {
            throw new IllegalStateException("Orchestrator is already built and cannot be reconfigured");
        }
    }
}