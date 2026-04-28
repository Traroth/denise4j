/*
 * OrchestratorBlackBoxTest.java
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
import fr.dufrenoy.imagefx.source.ImageSource;

import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Black-box tests for {@link Orchestrator}.
 */
class OrchestratorBlackBoxTest {

    // ─── Minimal valid pipeline ───────────────────────────────────────────────────

    private static EffectPipeline minimalPipeline() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        return new EffectPipeline()
                .addSource(new ImageSource(img))
                .build();
    }

    private static StagePool minimalPool() {
        return new StagePool(10, 10, 2, FrameDropPolicy.REPEAT_LAST);
    }

    // ─── build() validation ──────────────────────────────────────────────────────

    @Test
    void buildThrowsWhenPipelineNotSet() {
        assertThrows(IllegalStateException.class, () ->
                new Orchestrator()
                        .stagePool(minimalPool())
                        .targetFps(60)
                        .onFrame((i, d) -> {})
                        .build());
    }

    @Test
    void buildThrowsWhenStagePoolNotSet() {
        assertThrows(IllegalStateException.class, () ->
                new Orchestrator()
                        .pipeline(minimalPipeline())
                        .targetFps(60)
                        .onFrame((i, d) -> {})
                        .build());
    }

    @Test
    void buildThrowsWhenTargetFpsNotSet() {
        assertThrows(IllegalStateException.class, () ->
                new Orchestrator()
                        .pipeline(minimalPipeline())
                        .stagePool(minimalPool())
                        .onFrame((i, d) -> {})
                        .build());
    }

    @Test
    void buildThrowsWhenTargetFpsIsZero() {
        assertThrows(IllegalStateException.class, () ->
                new Orchestrator()
                        .pipeline(minimalPipeline())
                        .stagePool(minimalPool())
                        .targetFps(0)
                        .onFrame((i, d) -> {})
                        .build());
    }

    @Test
    void buildThrowsWhenTargetFpsIsNegative() {
        assertThrows(IllegalStateException.class, () ->
                new Orchestrator()
                        .pipeline(minimalPipeline())
                        .stagePool(minimalPool())
                        .targetFps(-1)
                        .onFrame((i, d) -> {})
                        .build());
    }

    @Test
    void buildThrowsWhenCallbackNotSet() {
        assertThrows(IllegalStateException.class, () ->
                new Orchestrator()
                        .pipeline(minimalPipeline())
                        .stagePool(minimalPool())
                        .targetFps(60)
                        .build());
    }

    @Test
    void pipelineAfterBuildThrows() {
        Orchestrator orchestrator = new Orchestrator()
                .pipeline(minimalPipeline())
                .stagePool(minimalPool())
                .targetFps(60)
                .onFrame((i, d) -> {})
                .build();
        assertThrows(IllegalStateException.class,
                () -> orchestrator.pipeline(minimalPipeline()));
    }

    @Test
    void stagePoolAfterBuildThrows() {
        Orchestrator orchestrator = new Orchestrator()
                .pipeline(minimalPipeline())
                .stagePool(minimalPool())
                .targetFps(60)
                .onFrame((i, d) -> {})
                .build();
        assertThrows(IllegalStateException.class,
                () -> orchestrator.stagePool(minimalPool()));
    }

    @Test
    void onFrameAfterBuildThrows() {
        Orchestrator orchestrator = new Orchestrator()
                .pipeline(minimalPipeline())
                .stagePool(minimalPool())
                .targetFps(60)
                .onFrame((i, d) -> {})
                .build();
        assertThrows(IllegalStateException.class,
                () -> orchestrator.onFrame((i, d) -> {}));
    }

    @Test
    void configurationAfterBuildThrows() {
        Orchestrator orchestrator = new Orchestrator()
                .pipeline(minimalPipeline())
                .stagePool(minimalPool())
                .targetFps(60)
                .onFrame((i, d) -> {})
                .build();
        assertThrows(IllegalStateException.class,
                () -> orchestrator.targetFps(30));
    }

    @Test
    void buildCalledTwiceThrows() {
        Orchestrator orchestrator = new Orchestrator()
                .pipeline(minimalPipeline())
                .stagePool(minimalPool())
                .targetFps(60)
                .onFrame((i, d) -> {})
                .build();
        assertThrows(IllegalStateException.class, orchestrator::build);
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Test
    void startBeforeBuildThrows() {
        assertThrows(IllegalStateException.class, () ->
                new Orchestrator().start());
    }

    @Test
    void doubleStartThrows() {
        Orchestrator orchestrator = new Orchestrator()
                .pipeline(minimalPipeline())
                .stagePool(minimalPool())
                .targetFps(60)
                .onFrame((i, d) -> {})
                .build();
        orchestrator.start();
        try {
            assertThrows(IllegalStateException.class, orchestrator::start);
        } finally {
            orchestrator.stop();
        }
    }

    @Test
    void stopWithoutStartThrows() {
        Orchestrator orchestrator = new Orchestrator()
                .pipeline(minimalPipeline())
                .stagePool(minimalPool())
                .targetFps(60)
                .onFrame((i, d) -> {})
                .build();
        assertThrows(IllegalStateException.class, orchestrator::stop);
    }

    @Test
    void startAndStopCompleteCleanly() throws InterruptedException {
        Orchestrator orchestrator = new Orchestrator()
                .pipeline(minimalPipeline())
                .stagePool(minimalPool())
                .targetFps(60)
                .onFrame((i, d) -> {})
                .build();
        orchestrator.start();
        Thread.sleep(50);
        orchestrator.stop(); // must not hang
    }

    // ─── Frame callback ──────────────────────────────────────────────────────────

    @Test
    void callbackIsInvokedWithIncreasingFrameIndex() throws Exception {
        CountDownLatch threeFrames = new CountDownLatch(3);
        AtomicLong lastIndex = new AtomicLong(-1);

        // N=4 so the render thread can complete 3 frames before blocking:
        // display(1) + ready(3) = 4 buffers, render thread blocks only on the 4th acquire.
        StagePool pool = new StagePool(10, 10, 4, FrameDropPolicy.REPEAT_LAST);

        Orchestrator orchestrator = new Orchestrator()
                .pipeline(minimalPipeline())
                .stagePool(pool)
                .targetFps(200)
                .onFrame((frameIndex, deltaMs) -> {
                    lastIndex.set(frameIndex);
                    threeFrames.countDown();
                })
                .build();

        orchestrator.start();
        boolean reached = threeFrames.await(2, TimeUnit.SECONDS);
        orchestrator.stop();

        assertTrue(reached, "Callback should be invoked at least 3 times");
        assertTrue(lastIndex.get() >= 2, "frameIndex should have reached at least 2");
    }

    @Test
    void callbackFirstFrameDeltaMsIsZero() throws Exception {
        AtomicLong firstDelta = new AtomicLong(-1);
        CountDownLatch firstFrame = new CountDownLatch(1);

        Orchestrator orchestrator = new Orchestrator()
                .pipeline(minimalPipeline())
                .stagePool(minimalPool())
                .targetFps(60)
                .onFrame((frameIndex, deltaMs) -> {
                    if (frameIndex == 0) {
                        firstDelta.set(deltaMs);
                        firstFrame.countDown();
                    }
                })
                .build();

        orchestrator.start();
        firstFrame.await(2, TimeUnit.SECONDS);
        orchestrator.stop();

        assertEquals(0L, firstDelta.get());
    }

    @Test
    void callbackFirstFrameIndexIsZero() throws Exception {
        AtomicLong firstIndex = new AtomicLong(-1);
        CountDownLatch firstFrame = new CountDownLatch(1);

        Orchestrator orchestrator = new Orchestrator()
                .pipeline(minimalPipeline())
                .stagePool(minimalPool())
                .targetFps(60)
                .onFrame((frameIndex, deltaMs) -> {
                    firstIndex.compareAndSet(-1, frameIndex);
                    firstFrame.countDown();
                })
                .build();

        orchestrator.start();
        firstFrame.await(2, TimeUnit.SECONDS);
        orchestrator.stop();

        assertEquals(0L, firstIndex.get());
    }

    // ─── getFrontBuffer ──────────────────────────────────────────────────────────

    @Test
    void getFrontBufferBeforeBuildThrows() {
        assertThrows(IllegalStateException.class,
                () -> new Orchestrator().getFrontBuffer());
    }

    @Test
    void getFrontBufferReturnsNonNullAfterFrameIsRendered() throws Exception {
        CountDownLatch oneFrame = new CountDownLatch(1);

        Orchestrator orchestrator = new Orchestrator()
                .pipeline(minimalPipeline())
                .stagePool(new StagePool(10, 10, 2, FrameDropPolicy.WAIT))
                .targetFps(60)
                .onFrame((i, d) -> oneFrame.countDown())
                .build();

        orchestrator.start();
        oneFrame.await(2, TimeUnit.SECONDS);

        Stage front = orchestrator.getFrontBuffer();
        orchestrator.stop();

        assertNotNull(front);
    }
}