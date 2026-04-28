/*
 * OrchestratorWhiteBoxTest.java
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

import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import fr.dufrenoy.imagefx.staging.EffectPipeline;
import fr.dufrenoy.imagefx.source.ImageSource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * White-box tests for {@link Orchestrator}.
 *
 * <p>These tests target internal implementation details not observable from
 * the public contract: the render thread name, and the interrupt-based
 * shutdown path when the render thread is blocked inside
 * {@link StagePool#acquireBackBuffer()}.</p>
 */
class OrchestratorWhiteBoxTest {

    private static EffectPipeline minimalPipeline() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        return new EffectPipeline()
                .addSource(new ImageSource(img))
                .build();
    }

    // ─── Render thread name ───────────────────────────────────────────────────────

    @Test
    void renderThreadIsNamedDenise4jRender() throws Exception {
        // Internal detail: renderThread is created with name "denise4j-render".
        // Risk: if the name changes or the thread is not the one calling the callback,
        // tooling (profilers, thread dumps) would show an anonymous thread.
        AtomicReference<String> threadName = new AtomicReference<>();
        CountDownLatch firstFrame = new CountDownLatch(1);

        Orchestrator orchestrator = new Orchestrator()
                .pipeline(minimalPipeline())
                .stagePool(new StagePool(10, 10, 2, FrameDropPolicy.REPEAT_LAST))
                .targetFps(200)
                .onFrame((i, d) -> {
                    threadName.compareAndSet(null, Thread.currentThread().getName());
                    firstFrame.countDown();
                })
                .build();

        orchestrator.start();
        firstFrame.await(2, TimeUnit.SECONDS);
        orchestrator.stop();

        assertEquals("denise4j-render", threadName.get(),
                "Render thread must be named 'denise4j-render'");
    }

    // ─── stop() unblocks render thread blocked on acquireBackBuffer ───────────────

    @Test
    void stopUnblocksRenderThreadBlockedOnAcquireBackBuffer() throws Exception {
        // Internal path: stop() sets running=false and calls interrupt().
        // If the render thread is blocked in freeQueue.take() (acquireBackBuffer),
        // the interrupt unblocks it; the catch(InterruptedException) breaks the loop.
        // Risk: if the interrupt is not forwarded correctly, stop() hangs in join().
        //
        // Setup: N=2 pool with N-1=1 free slot. The render thread acquires it on frame 0,
        // presents it, then tries to acquire again but the display thread never calls
        // getFrontBuffer(), so freeQueue is empty and the render thread blocks.
        CountDownLatch frame0Presented = new CountDownLatch(1);

        StagePool pool = new StagePool(10, 10, 2, FrameDropPolicy.REPEAT_LAST) {
            // We use REPEAT_LAST on the display side; we just never call getFrontBuffer().
        };

        Orchestrator orchestrator = new Orchestrator()
                .pipeline(minimalPipeline())
                .stagePool(pool)
                .targetFps(200)
                .onFrame((i, d) -> {
                    if (i == 0) {
                        frame0Presented.countDown();
                    }
                })
                .build();

        orchestrator.start();
        // Wait until frame 0 has been rendered and presented.
        assertTrue(frame0Presented.await(2, TimeUnit.SECONDS),
                "Frame 0 should complete before the render thread blocks");

        // Give the render thread a moment to call acquireBackBuffer() and block.
        Thread.sleep(50);

        // stop() must return within a reasonable time, not hang.
        long before = System.currentTimeMillis();
        orchestrator.stop();
        long elapsed = System.currentTimeMillis() - before;

        assertTrue(elapsed < 2000,
                "stop() should return quickly even when render thread is blocked on acquire");
    }
}