/*
 * ParamDoubleBlackBoxTest.java
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Black-box tests for {@link ParamDouble}.
 *
 * <p>These tests verify the public contract of {@code ParamDouble} as
 * specified by its Javadoc and JML annotations, with no knowledge
 * of internal implementation.</p>
 */
class ParamDoubleBlackBoxTest {

    private static final double DELTA = 1e-15;

    // ─── Constructor ─────────────────────────────────────────────────────────────

    @Test
    void constructorWithZeroExposesZero() {
        ParamDouble param = new ParamDouble(0.0);
        assertEquals(0.0, param.get(), DELTA);
    }

    @Test
    void constructorWithPositiveExposesGivenValue() {
        ParamDouble param = new ParamDouble(1.5);
        assertEquals(1.5, param.get(), DELTA);
    }

    @Test
    void constructorWithNegativeExposesGivenValue() {
        ParamDouble param = new ParamDouble(-3.14);
        assertEquals(-3.14, param.get(), DELTA);
    }

    @Test
    void constructorWithDoubleMinValueExposesDoubleMinValue() {
        ParamDouble param = new ParamDouble(Double.MIN_VALUE);
        assertEquals(Double.MIN_VALUE, param.get(), DELTA);
    }

    @Test
    void constructorWithDoubleMaxValueExposesDoubleMaxValue() {
        ParamDouble param = new ParamDouble(Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, param.get(), DELTA);
    }

    @Test
    void constructorWithPositiveInfinityExposesPositiveInfinity() {
        ParamDouble param = new ParamDouble(Double.POSITIVE_INFINITY);
        assertTrue(Double.isInfinite(param.get()) && param.get() > 0);
    }

    @Test
    void constructorWithNaNExposesNaN() {
        ParamDouble param = new ParamDouble(Double.NaN);
        assertTrue(Double.isNaN(param.get()));
    }

    // ─── set() ───────────────────────────────────────────────────────────────────

    @Test
    void setReplacesCurrentValue() {
        ParamDouble param = new ParamDouble(1.0);
        param.set(2.5);
        assertEquals(2.5, param.get(), DELTA);
    }

    @Test
    void setToZeroReplacesCurrentValue() {
        ParamDouble param = new ParamDouble(99.9);
        param.set(0.0);
        assertEquals(0.0, param.get(), DELTA);
    }

    @Test
    void setToNegativeReplacesCurrentValue() {
        ParamDouble param = new ParamDouble(1.0);
        param.set(-1.0);
        assertEquals(-1.0, param.get(), DELTA);
    }

    @Test
    void setIsIdempotentWhenCalledTwiceWithSameValue() {
        ParamDouble param = new ParamDouble(0.0);
        param.set(3.0);
        param.set(3.0);
        assertEquals(3.0, param.get(), DELTA);
    }

    @Test
    void setOverwritesAPreviousSet() {
        ParamDouble param = new ParamDouble(0.0);
        param.set(3.0);
        param.set(4.0);
        assertEquals(4.0, param.get(), DELTA);
    }

    // ─── add() ───────────────────────────────────────────────────────────────────

    @Test
    void addPositiveDeltaIncreasesValue() {
        ParamDouble param = new ParamDouble(1.0);
        param.add(0.5);
        assertEquals(1.5, param.get(), DELTA);
    }

    @Test
    void addNegativeDeltaDecreasesValue() {
        ParamDouble param = new ParamDouble(1.0);
        param.add(-0.25);
        assertEquals(0.75, param.get(), DELTA);
    }

    @Test
    void addZeroLeavesValueUnchanged() {
        ParamDouble param = new ParamDouble(3.14);
        param.add(0.0);
        assertEquals(3.14, param.get(), DELTA);
    }

    @Test
    void addIsCumulativeAcrossMultipleCalls() {
        ParamDouble param = new ParamDouble(0.0);
        param.add(0.1);
        param.add(0.2);
        param.add(0.3);
        assertEquals(0.6, param.get(), 1e-10);
    }

    @Test
    void addAndSetCanBeMixed() {
        ParamDouble param = new ParamDouble(1.0);
        param.add(0.5);
        param.set(10.0);
        param.add(-1.0);
        assertEquals(9.0, param.get(), DELTA);
    }
}