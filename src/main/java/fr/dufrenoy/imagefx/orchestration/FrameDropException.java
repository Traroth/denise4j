/*
 * FrameDropException.java
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
 * Thrown by {@link StagePool#getFrontBuffer()} when
 * {@link FrameDropPolicy#EXCEPTION} is active and the display thread
 * requests a frame that has not yet been produced by the render thread.
 */
public class FrameDropException extends RuntimeException {

    /**
     * Constructs a new {@code FrameDropException} with a default message.
     */
    public FrameDropException() {
        super("Frame drop: no rendered frame is available for display");
    }
}