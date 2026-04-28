/*
 * StagePoolBlackBoxTest.java
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Black-box tests for {@link StagePool}.
 */
class StagePoolBlackBoxTest {

    // ─── Constructor ─────────────────────────────────────────────────────────────

    @Test
    void constructorStoresDimensions() {
        StagePool pool = new StagePool(320, 200, 2, FrameDropPolicy.REPEAT_LAST);
        assertEquals(320, pool.getWidth());
        assertEquals(200, pool.getHeight());
    }

    @Test
    void constructorStoresBufferCount() {
        StagePool pool = new StagePool(100, 100, 3, FrameDropPolicy.REPEAT_LAST);
        assertEquals(3, pool.getBufferCount());
    }

    @Test
    void constructorStoresFrameDropPolicy() {
        StagePool pool = new StagePool(100, 100, 2, FrameDropPolicy.EXCEPTION);
        assertEquals(FrameDropPolicy.EXCEPTION, pool.getFrameDropPolicy());
    }

    @Test
    void constructorRejectsZeroWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> new StagePool(0, 100, 2, FrameDropPolicy.REPEAT_LAST));
    }

    @Test
    void constructorRejectsNegativeWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> new StagePool(-1, 100, 2, FrameDropPolicy.REPEAT_LAST));
    }

    @Test
    void constructorRejectsZeroHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> new StagePool(100, 0, 2, FrameDropPolicy.REPEAT_LAST));
    }

    @Test
    void constructorRejectsNegativeHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> new StagePool(100, -1, 2, FrameDropPolicy.REPEAT_LAST));
    }

    @Test
    void constructorRejectsBufferCountOfOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new StagePool(100, 100, 1, FrameDropPolicy.REPEAT_LAST));
    }

    @Test
    void constructorRejectsBufferCountOfZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new StagePool(100, 100, 0, FrameDropPolicy.REPEAT_LAST));
    }

    @Test
    void constructorRejectsNegativeBufferCount() {
        assertThrows(IllegalArgumentException.class,
                () -> new StagePool(100, 100, -1, FrameDropPolicy.REPEAT_LAST));
    }

    @Test
    void constructorRejectsNullPolicy() {
        assertThrows(NullPointerException.class,
                () -> new StagePool(100, 100, 2, null));
    }

    @Test
    void constructorAcceptsBufferCountOfTwo() {
        StagePool pool = new StagePool(100, 100, 2, FrameDropPolicy.REPEAT_LAST);
        assertEquals(2, pool.getBufferCount());
    }

    @Test
    void constructorAcceptsBufferCountOfThree() {
        StagePool pool = new StagePool(100, 100, 3, FrameDropPolicy.REPEAT_LAST);
        assertEquals(3, pool.getBufferCount());
    }

    // ─── acquireBackBuffer ───────────────────────────────────────────────────────

    @Test
    void acquireBackBufferReturnsNonNull() throws InterruptedException {
        StagePool pool = new StagePool(100, 100, 2, FrameDropPolicy.REPEAT_LAST);
        Stage back = pool.acquireBackBuffer();
        assertNotNull(back);
    }

    @Test
    void acquireBackBufferReturnsStageWithCorrectDimensions() throws InterruptedException {
        StagePool pool = new StagePool(320, 200, 2, FrameDropPolicy.REPEAT_LAST);
        Stage back = pool.acquireBackBuffer();
        assertEquals(320, back.getWidth());
        assertEquals(200, back.getHeight());
    }

    @Test
    void doubleBufferingAllowsOneAcquireBeforeBlocking() throws Exception {
        StagePool pool = new StagePool(10, 10, 2, FrameDropPolicy.REPEAT_LAST);
        pool.acquireBackBuffer(); // should succeed (1 free stage with N=2)

        // Second acquire must block — verify with a timeout
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

        boolean unblocked = acquired.await(200, TimeUnit.MILLISECONDS);
        t.interrupt();
        assertTrue(!unblocked, "Second acquireBackBuffer() should block with N=2");
    }

    @Test
    void tripleBufferingAllowsTwoAcquiresBeforeBlocking() throws Exception {
        StagePool pool = new StagePool(10, 10, 3, FrameDropPolicy.REPEAT_LAST);
        Stage back1 = pool.acquireBackBuffer();
        pool.present(back1);
        Stage back2 = pool.acquireBackBuffer(); // second acquire should not block

        // Third acquire must block
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

        boolean unblocked = acquired.await(200, TimeUnit.MILLISECONDS);
        t.interrupt();
        assertTrue(!unblocked, "Third acquireBackBuffer() should block with N=3");
    }

    // ─── present / getFrontBuffer cycle ──────────────────────────────────────────

    @Test
    void presentedStageBecomesNextFrontBuffer() throws InterruptedException {
        StagePool pool = new StagePool(10, 10, 2, FrameDropPolicy.REPEAT_LAST);
        Stage back = pool.acquireBackBuffer();
        pool.present(back);

        Stage front = pool.getFrontBuffer();
        assertSame(back, front);
    }

    @Test
    void presentedStagesAreReturnedInOrder() throws InterruptedException {
        StagePool pool = new StagePool(10, 10, 3, FrameDropPolicy.REPEAT_LAST);

        Stage back1 = pool.acquireBackBuffer();
        pool.present(back1);
        Stage back2 = pool.acquireBackBuffer();
        pool.present(back2);

        assertSame(back1, pool.getFrontBuffer());
        assertSame(back2, pool.getFrontBuffer());
    }

    @Test
    void getFrontBufferFreesOldDisplayStageForReuse() throws InterruptedException {
        // With N=2: acquire, present, getFrontBuffer should free the old display stage.
        StagePool pool = new StagePool(10, 10, 2, FrameDropPolicy.REPEAT_LAST);

        Stage back1 = pool.acquireBackBuffer();
        pool.present(back1);
        pool.getFrontBuffer();             // old display stage → free
        Stage back2 = pool.acquireBackBuffer(); // should not block
        assertNotNull(back2);
    }

    // ─── FrameDropPolicy ─────────────────────────────────────────────────────────

    @Test
    void getFrontBufferWithNoFrameReadyAndExceptionPolicyThrows() {
        StagePool pool = new StagePool(10, 10, 2, FrameDropPolicy.EXCEPTION);
        assertThrows(FrameDropException.class, pool::getFrontBuffer);
    }

    @Test
    void getFrontBufferWithNoFrameReadyAndRepeatLastPolicyReturnsLastStage() {
        StagePool pool = new StagePool(10, 10, 2, FrameDropPolicy.REPEAT_LAST);
        Stage first = pool.getFrontBuffer(); // no frame ready → repeat last (initial display stage)
        assertNotNull(first);
        Stage second = pool.getFrontBuffer(); // still no new frame
        assertSame(first, second);
    }

    @Test
    void getFrontBufferWithNoFrameReadyAndWarnAndRepeatPolicyReturnsLastStage() {
        StagePool pool = new StagePool(10, 10, 2, FrameDropPolicy.WARN_AND_REPEAT);
        Stage stage = pool.getFrontBuffer();
        assertNotNull(stage);
    }

    @Test
    void getFrontBufferWithNoFrameReadyAndSkipPolicyReturnsNull() {
        StagePool pool = new StagePool(10, 10, 2, FrameDropPolicy.SKIP);
        Stage stage = pool.getFrontBuffer();
        assertNull(stage);
    }

    @Test
    void getFrontBufferWithWaitPolicyBlocksUntilFrameIsPresented() throws Exception {
        StagePool pool = new StagePool(10, 10, 2, FrameDropPolicy.WAIT);

        AtomicReference<Stage> result = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Thread display = new Thread(() -> {
            result.set(pool.getFrontBuffer()); // should block
            done.countDown();
        });
        display.setDaemon(true);
        display.start();

        // Verify the display thread is blocked
        boolean completedEarly = done.await(150, TimeUnit.MILLISECONDS);
        assertTrue(!completedEarly, "WAIT policy should block until a frame is presented");

        // Now produce a frame
        Stage back = pool.acquireBackBuffer();
        pool.present(back);

        boolean completed = done.await(500, TimeUnit.MILLISECONDS);
        assertTrue(completed, "WAIT policy should unblock after present()");
        assertSame(back, result.get());
    }

    @Test
    void getFrontBufferWithExceptionPolicyAndFrameReadyDoesNotThrow() throws InterruptedException {
        StagePool pool = new StagePool(10, 10, 2, FrameDropPolicy.EXCEPTION);
        Stage back = pool.acquireBackBuffer();
        pool.present(back);
        Stage front = pool.getFrontBuffer(); // frame is ready — must not throw
        assertNotNull(front);
    }

    @Test
    void getFrontBufferNominalReturnsStageWithCorrectDimensions() throws InterruptedException {
        StagePool pool = new StagePool(320, 200, 2, FrameDropPolicy.REPEAT_LAST);
        Stage back = pool.acquireBackBuffer();
        pool.present(back);
        Stage front = pool.getFrontBuffer();
        assertEquals(320, front.getWidth());
        assertEquals(200, front.getHeight());
    }

    // ─── present validation ───────────────────────────────────────────────────────

    @Test
    void presentRejectsNull() {
        StagePool pool = new StagePool(10, 10, 2, FrameDropPolicy.REPEAT_LAST);
        assertThrows(NullPointerException.class, () -> pool.present(null));
    }
}