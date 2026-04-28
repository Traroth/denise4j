/*
 * FrameCallback.java
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
 * Called by {@link Orchestrator} once per frame, before rendering, on the
 * render thread.
 *
 * <p>Implementations should update {@link ParamInt} and {@link ParamDouble}
 * values that drive the {@link EffectPipeline}. All parameter mutations must
 * occur inside this callback; mutating parameters from any other thread is
 * not supported.</p>
 */
@FunctionalInterface
public interface FrameCallback {

    /**
     * Invoked once per frame before the pipeline renders.
     *
     * @param frameIndex the zero-based index of the frame about to be rendered
     * @param deltaMs    elapsed time in milliseconds since the previous frame
     *                   started (0 on the first frame)
     */
    void onFrame(long frameIndex, long deltaMs);
}