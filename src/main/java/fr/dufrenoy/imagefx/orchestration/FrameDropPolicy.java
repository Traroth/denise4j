/*
 * FrameDropPolicy.java
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

/**
 * Strategy applied by {@link StagePool#getFrontBuffer()} when the display
 * thread requests the next frame but the render thread has not yet completed
 * it.
 *
 * <p>A frame drop occurs when the display tick fires faster than the renderer
 * can produce frames. The choice of policy trades off between latency, visual
 * smoothness, error visibility, and blocking behaviour.</p>
 */
public enum FrameDropPolicy {

    /**
     * Throw a {@link FrameDropException}.
     *
     * <p>Useful during development and testing to surface timing problems
     * immediately.</p>
     */
    EXCEPTION,

    /**
     * Block the calling thread until the next frame is ready.
     *
     * <p>Guarantees that every rendered frame is displayed exactly once,
     * at the cost of stalling the display thread.</p>
     */
    WAIT,

    /**
     * Return the previous front buffer without advancing.
     *
     * <p>The frame that was last displayed is shown again. Silent — no
     * logging is performed.</p>
     */
    REPEAT_LAST,

    /**
     * Log a warning to {@code System.err} then return the previous front
     * buffer without advancing, like {@link #REPEAT_LAST}.
     *
     * <p>Useful during development to detect frame-rate problems while
     * keeping the animation running.</p>
     */
    WARN_AND_REPEAT,

    /**
     * Return {@code null} without advancing.
     *
     * <p>The caller is responsible for handling the {@code null} return
     * value, for instance by skipping the draw call entirely.</p>
     */
    SKIP
}