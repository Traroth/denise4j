/*
 * StagePool.java
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

import fr.dufrenoy.imagefx.staging.Stage;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A thread-safe pool of {@link Stage} instances for double or triple
 * buffering.
 *
 * <p>The pool manages {@code bufferCount} stages as a bounded producer–consumer
 * pipeline:</p>
 * <ul>
 *   <li>The <em>render thread</em> calls {@link #acquireBackBuffer()} to obtain
 *       a free stage, renders into it, then calls {@link #present(Stage)} to
 *       enqueue it for display.</li>
 *   <li>The <em>display thread</em> calls {@link #getFrontBuffer()} to advance
 *       to the next completed frame.</li>
 * </ul>
 *
 * <p>{@code acquireBackBuffer()} blocks when all stages are either displayed
 * or queued for display, which naturally throttles the render thread to at
 * most one frame ahead of the display thread. With two buffers this means the
 * renderer must wait for every display tick; with three buffers it may render
 * one frame ahead.</p>
 *
 * <p>When {@code getFrontBuffer()} is called and no new frame is ready, the
 * configured {@link FrameDropPolicy} determines the behaviour.</p>
 *
 * <p>This class is safe for use by exactly one render thread and one display
 * thread concurrently. It has no dependency on any GUI toolkit.</p>
 */
public class StagePool {

    /*@
      @ public invariant getWidth() > 0;
      @ public invariant getHeight() > 0;
      @ public invariant getBufferCount() >= 2;
      @ public invariant getFrameDropPolicy() != null;
      @*/

    // ─── Instance variables ──────────────────────────────────────────────────────

    private final int width;
    private final int height;
    private final int bufferCount;
    private final FrameDropPolicy frameDropPolicy;

    private final BlockingQueue<Stage> freeQueue;
    private final BlockingQueue<Stage> readyQueue;
    private volatile Stage displayStage;

    // ─── Constructors ────────────────────────────────────────────────────────────

    /**
     * Creates a new pool with the given dimensions, buffer count, and frame
     * drop policy.
     *
     * <p>One stage is reserved as the initial front buffer (blank, filled with
     * opaque black). The remaining {@code bufferCount - 1} stages are
     * immediately available for rendering.</p>
     *
     * @param width           width of each stage in pixels
     * @param height          height of each stage in pixels
     * @param bufferCount     number of stages to allocate; must be at least 2
     * @param frameDropPolicy policy to apply when {@link #getFrontBuffer()} is
     *                        called but no new frame is ready
     * @throws IllegalArgumentException if {@code width} or {@code height} is
     *         not strictly positive, or if {@code bufferCount} is less than 2
     * @throws NullPointerException if {@code frameDropPolicy} is {@code null}
     */
    //@ requires width > 0;
    //@ requires height > 0;
    //@ requires bufferCount >= 2;
    //@ requires frameDropPolicy != null;
    //@ ensures getWidth() == width;
    //@ ensures getHeight() == height;
    //@ ensures getBufferCount() == bufferCount;
    //@ ensures getFrameDropPolicy() == frameDropPolicy;
    public StagePool(int width, int height, int bufferCount, FrameDropPolicy frameDropPolicy) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Stage dimensions must be strictly positive: " + width + "x" + height);
        }
        if (bufferCount < 2) {
            throw new IllegalArgumentException(
                    "Buffer count must be at least 2, got: " + bufferCount);
        }
        if (frameDropPolicy == null) {
            throw new NullPointerException("frameDropPolicy must not be null");
        }
        this.width = width;
        this.height = height;
        this.bufferCount = bufferCount;
        this.frameDropPolicy = frameDropPolicy;

        this.freeQueue = new ArrayBlockingQueue<>(bufferCount - 1);
        this.readyQueue = new ArrayBlockingQueue<>(bufferCount - 1);

        this.displayStage = new Stage(width, height);
        for (int i = 1; i < bufferCount; i++) {
            freeQueue.add(new Stage(width, height));
        }
    }

    // ─── Render-thread API ───────────────────────────────────────────────────────

    /**
     * Acquires a free stage for rendering.
     *
     * <p>Blocks until a stage becomes available. A stage becomes available
     * when the display thread consumes a previously presented frame via
     * {@link #getFrontBuffer()}.</p>
     *
     * <p>Must be called from the render thread only. The returned stage must
     * be passed back to {@link #present(Stage)} and must not be used after
     * that call.</p>
     *
     * @return a stage ready to be rendered into, never {@code null}
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    //@ ensures \result != null;
    //@ ensures \result.getWidth() == getWidth();
    //@ ensures \result.getHeight() == getHeight();
    public Stage acquireBackBuffer() throws InterruptedException {
        return freeQueue.take();
    }

    /**
     * Enqueues a fully rendered stage for display.
     *
     * <p>Must be called from the render thread only, with the stage that was
     * previously returned by {@link #acquireBackBuffer()}. After this call the
     * caller must not access the stage until it is acquired again.</p>
     *
     * @param stage the rendered stage to enqueue; must have been acquired via
     *              {@link #acquireBackBuffer()}
     * @throws NullPointerException if {@code stage} is {@code null}
     */
    //@ requires stage != null;
    //@ ensures \not_assigned(width, height, bufferCount, frameDropPolicy);
    public void present(Stage stage) {
        if (stage == null) {
            throw new NullPointerException("stage must not be null");
        }
        readyQueue.add(stage);
    }

    // ─── Display-thread API ──────────────────────────────────────────────────────

    /**
     * Advances to the next ready frame and returns the front buffer.
     *
     * <p>If a rendered frame is available, it becomes the new front buffer and
     * the previous front buffer is returned to the free pool. If no rendered
     * frame is available, the configured {@link FrameDropPolicy} determines
     * the outcome:</p>
     * <ul>
     *   <li>{@link FrameDropPolicy#EXCEPTION} — throws {@link FrameDropException}</li>
     *   <li>{@link FrameDropPolicy#WAIT} — blocks until a frame is ready</li>
     *   <li>{@link FrameDropPolicy#REPEAT_LAST} — returns the current front
     *       buffer silently</li>
     *   <li>{@link FrameDropPolicy#WARN_AND_REPEAT} — logs to {@code System.err}
     *       then returns the current front buffer</li>
     *   <li>{@link FrameDropPolicy#SKIP} — returns {@code null}</li>
     * </ul>
     *
     * <p>Must be called from the display thread only.</p>
     *
     * @return the front buffer to display, or {@code null} if the policy is
     *         {@link FrameDropPolicy#SKIP} and no frame is ready
     * @throws FrameDropException if the policy is
     *         {@link FrameDropPolicy#EXCEPTION} and no frame is ready
     */
    //@ ensures getFrameDropPolicy() != FrameDropPolicy.SKIP ==> \result != null;
    //@ ensures getFrameDropPolicy() != FrameDropPolicy.SKIP ==> \result.getWidth() == getWidth();
    //@ ensures getFrameDropPolicy() != FrameDropPolicy.SKIP ==> \result.getHeight() == getHeight();
    //@ signals (FrameDropException) getFrameDropPolicy() == FrameDropPolicy.EXCEPTION;
    public Stage getFrontBuffer() {
        Stage next = readyQueue.poll();
        if (next != null) {
            Stage old = displayStage;
            displayStage = next;
            freeQueue.add(old);
            return next;
        }
        return applyFrameDropPolicy();
    }

    // ─── Accessors ───────────────────────────────────────────────────────────────

    /**
     * Returns the width of each stage in this pool.
     *
     * @return the width in pixels, always strictly positive
     */
    //@ ensures \result > 0;
    /*@ pure @*/ public int getWidth() {
        return width;
    }

    /**
     * Returns the height of each stage in this pool.
     *
     * @return the height in pixels, always strictly positive
     */
    //@ ensures \result > 0;
    /*@ pure @*/ public int getHeight() {
        return height;
    }

    /**
     * Returns the number of stages in this pool.
     *
     * @return the buffer count, always at least 2
     */
    //@ ensures \result >= 2;
    /*@ pure @*/ public int getBufferCount() {
        return bufferCount;
    }

    /**
     * Returns the frame drop policy of this pool.
     *
     * @return the policy, never {@code null}
     */
    //@ ensures \result != null;
    /*@ pure @*/ public FrameDropPolicy getFrameDropPolicy() {
        return frameDropPolicy;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    private Stage applyFrameDropPolicy() {
        switch (frameDropPolicy) {
            case EXCEPTION:
                throw new FrameDropException();
            case WAIT:
                try {
                    Stage next = readyQueue.take();
                    Stage old = displayStage;
                    displayStage = next;
                    freeQueue.add(old);
                    return next;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return displayStage;
                }
            case REPEAT_LAST:
                return displayStage;
            case WARN_AND_REPEAT:
                System.err.println("[denise4j] WARNING: frame drop detected");
                return displayStage;
            case SKIP:
                return null;
            default:
                throw new IllegalStateException("Unknown FrameDropPolicy: " + frameDropPolicy);
        }
    }
}