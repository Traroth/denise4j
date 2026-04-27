/*
 * ParamDouble.java
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
 * A mutable double parameter for the effect pipeline.
 *
 * <p>Used for continuous values such as zoom factors and rotation angles.
 * The pipeline structure is built once and reused across frames;
 * only the {@code ParamDouble} values change between frames.</p>
 */
public class ParamDouble {

    // ─── Instance variables ──────────────────────────────────────────────────────

    private double value;

    // ─── Constructors ────────────────────────────────────────────────────────────

    /**
     * Creates a new parameter with the given initial value.
     *
     * @param initialValue the initial value
     */
    //@ ensures get() == initialValue;
    public ParamDouble(double initialValue) {
        this.value = initialValue;
    }

    // ─── Accessors ───────────────────────────────────────────────────────────────

    /**
     * Returns the current value.
     *
     * @return the current value
     */
    /*@ pure @*/ public double get() {
        return value;
    }

    // ─── Mutators ────────────────────────────────────────────────────────────────

    /**
     * Sets the value.
     *
     * @param value the new value
     */
    //@ ensures get() == value;
    public void set(double value) {
        this.value = value;
    }

    /**
     * Adds the given delta to the current value.
     *
     * @param delta the value to add (may be negative)
     */
    //@ ensures get() == \old(get()) + delta;
    public void add(double delta) {
        this.value += delta;
    }
}