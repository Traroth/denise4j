/*
 * StagePoolWhiteBoxTest.java
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import fr.dufrenoy.imagefx.staging.Stage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * White-box tests for {@link StagePool}.
 *
 * <p>These tests target internal implementation paths not visible from the
 * public contract: stage conservation across cycles, the exact object identity
 * of freed stages, and interrupt handling inside {@link FrameDropPolicy#WAIT}.</p>
 */
class StagePoolWhiteBoxTest {

    // ─── Stage conservation ───────────────────────────────────────────────────────

    @Test
    void stagesAreConservedAcrossMultipleCyclesWithN2() throws InterruptedException {
        // With N=2: freeQueue capacity=1, readyQueue capacity=1.
        // Each cycle: acquire → present → getFrontBuffer must keep the pool live.
        // Risk: if a stage leaks (not returned to freeQueue), a later acquire blocks forever.
        StagePool pool = new StagePool(10, 10, 2, FrameDropPolicy.REPEAT_LAST);

        for (int i = 0; i < 5; i++) {
            Stage back = pool.acquireBackBuffer();
            pool.present(back);
            pool.getFrontBuffer();
        }
        // If the invariant held, acquireBackBuffer must not block here.
        CountDownLatch acquired = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            try {
                pool.acquireBackBuffer();
                acquired.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.setDaemon(true);
        t.start();
        assertTrue(acquired.await(500, TimeUnit.MILLISECONDS),
                "acquireBackBuffer should not block after N cycles: stage leaked from pool");
        t.interrupt();
    }

    @Test
    void stagesAreConservedAcrossMultipleCyclesWithN3() throws InterruptedException {
        // With N=3: the render thread can be one frame ahead.
        // Risk: triple-buffering is more complex; stages must still balance.
        StagePool pool = new StagePool(10, 10, 3, FrameDropPolicy.REPEAT_LAST);

        for (int i = 0; i < 5; i++) {
            Stage back1 = pool.acquireBackBuffer();
            pool.present(back1);
            Stage back2 = pool.acquireBackBuffer();
            pool.present(back2);
            pool.getFrontBuffer();
            pool.getFrontBuffer();
        }

        CountDownLatch acquired = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            try {
                pool.acquireBackBuffer();
                acquired.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.setDaemon(true);
        t.start();
        assertTrue(acquired.await(500, TimeUnit.MILLISECONDS),
                "acquireBackBuffer should not block after N cycles with N=3");
        t.interrupt();
    }

    // ─── Old displayStage is freed to freeQueue ───────────────────────────────────

    @Test
    void getFrontBufferReturnsOldDisplayStageToFreeQueue() throws InterruptedException {
        // Internal path: getFrontBuffer() does freeQueue.add(old displayStage).
        // With N=3 after acquire+present+getFrontBuffer, freeQueue must have 2 slots again,
        // so two consecutive acquires must both succeed without blocking.
        // Risk: if the swap is done wrong, the old displayStage leaks.
        StagePool pool = new StagePool(10, 10, 3, FrameDropPolicy.REPEAT_LAST);

        Stage back1 = pool.acquireBackBuffer(); // freeQueue now has 1 free slot
        pool.present(back1);
        pool.getFrontBuffer();                  // back1 becomes display; initial stage freed

        // Now freeQueue must have 2 free slots again (initial + back1 swapped out below would
        // actually be: after getFrontBuffer, old displayStage (initial) goes to freeQueue).
        // So two acquires must both succeed.
        Stage a = pool.acquireBackBuffer();
        Stage b = pool.acquireBackBuffer();     // would block if old displayStage wasn't freed
        assertNotNull(a);
        assertNotNull(b);
        assertNotSame(a, b);
    }

    // ─── WAIT policy — interrupt handling ─────────────────────────────────────────

    @Test
    void waitPolicyInterruptedReturnsPreviousDisplayStage() throws Exception {
        // Internal path: applyFrameDropPolicy() WAIT branch catches InterruptedException,
        // re-interrupts the thread, and returns displayStage (not null, not a new frame).
        // Risk: if the catch block forgets to return displayStage, it falls through to null.
        StagePool pool = new StagePool(10, 10, 2, FrameDropPolicy.WAIT);

        // Record the display stage before any frame is produced.
        // Since no present() has been called, getFrontBuffer() will block (WAIT policy).
        AtomicReference<Stage> returnedStage = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Thread display = new Thread(() -> {
            returnedStage.set(pool.getFrontBuffer()); // blocks on readyQueue.take()
            done.countDown();
        });
        display.setDaemon(true);
        display.start();

        // Confirm it is blocking, then interrupt it.
        boolean finishedEarly = done.await(100, TimeUnit.MILLISECONDS);
        assertTrue(!finishedEarly, "Display thread should be blocked under WAIT policy");

        display.interrupt();
        assertTrue(done.await(500, TimeUnit.MILLISECONDS), "Interrupted display thread should unblock");

        // The result must be the initial display stage (non-null), not a presented frame.
        assertNotNull(returnedStage.get(),
                "WAIT interrupted: displayStage must be returned, not null");
    }

    @Test
    void waitPolicyInterruptedReturnsSameObjectAsInitialDisplayStage() throws Exception {
        // Refines the previous test: the returned stage must be the exact same object
        // as the one that was used as initial display stage.
        // Obtain the initial display stage via a full cycle first (present then getFrontBuffer),
        // which frees the initial stage to freeQueue; acquire it to identify it.
        StagePool pool = new StagePool(10, 10, 2, FrameDropPolicy.WAIT);

        // Produce one frame so the initial display stage is freed and we can grab it.
        Stage back = pool.acquireBackBuffer();      // takes the one free slot
        pool.present(back);
        Stage front = pool.getFrontBuffer();        // back becomes display; initial → freeQueue
        Stage initialStage = pool.acquireBackBuffer(); // this is the initial display stage
        // Now freeQueue is empty; present initialStage to restore and make back the display.
        pool.present(initialStage);
        pool.getFrontBuffer();                      // initialStage is now displayStage

        AtomicReference<Stage> returnedStage = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Thread display = new Thread(() -> {
            returnedStage.set(pool.getFrontBuffer());
            done.countDown();
        });
        display.setDaemon(true);
        display.start();

        done.await(100, TimeUnit.MILLISECONDS); // let it block
        display.interrupt();
        done.await(500, TimeUnit.MILLISECONDS);

        assertSame(initialStage, returnedStage.get(),
                "WAIT interrupted: must return the current displayStage object");
    }
}