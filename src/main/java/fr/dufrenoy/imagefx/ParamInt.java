/*
 * ParamInt.java
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
package fr.dufrenoy.imagefx;

/**
 * A mutable integer parameter for the effect pipeline.
 *
 * <p>Used for discrete values such as pixel offsets and tile indices.
 * The pipeline structure is built once and reused across frames;
 * only the {@code ParamInt} values change between frames.</p>
 */
public class ParamInt {

    // ─── Instance variables ──────────────────────────────────────────────────────

    private int value;

    // ─── Constructors ────────────────────────────────────────────────────────────

    /**
     * Creates a new parameter with the given initial value.
     *
     * @param initialValue the initial value
     */
    //@ ensures get() == initialValue;
    public ParamInt(int initialValue) {
        this.value = initialValue;
    }

    // ─── Accessors ───────────────────────────────────────────────────────────────

    /**
     * Returns the current value.
     *
     * @return the current value
     */
    /*@ pure @*/ public int get() {
        return value;
    }

    // ─── Mutators ────────────────────────────────────────────────────────────────

    /**
     * Sets the value.
     *
     * @param value the new value
     */
    //@ ensures get() == value;
    public void set(int value) {
        this.value = value;
    }

    /**
     * Adds the given delta to the current value.
     *
     * @param delta the value to add (may be negative)
     */
    //@ ensures get() == \old(get()) + delta;
    public void add(int delta) {
        this.value += delta;
    }
}