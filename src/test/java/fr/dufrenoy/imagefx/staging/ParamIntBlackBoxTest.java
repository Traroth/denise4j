/*
 * ParamIntBlackBoxTest.java
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
package fr.dufrenoy.imagefx.staging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Black-box tests for {@link ParamInt}.
 *
 * <p>These tests verify the public contract of {@code ParamInt} as
 * specified by its Javadoc and JML annotations, with no knowledge
 * of internal implementation.</p>
 */
class ParamIntBlackBoxTest {

    // ─── Constructor ─────────────────────────────────────────────────────────────

    @Test
    void constructorWithZeroExposesZero() {
        ParamInt param = new ParamInt(0);
        assertEquals(0, param.get());
    }

    @Test
    void constructorWithPositiveExposesGivenValue() {
        ParamInt param = new ParamInt(42);
        assertEquals(42, param.get());
    }

    @Test
    void constructorWithNegativeExposesGivenValue() {
        ParamInt param = new ParamInt(-17);
        assertEquals(-17, param.get());
    }

    @Test
    void constructorWithIntMinValueExposesIntMinValue() {
        ParamInt param = new ParamInt(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, param.get());
    }

    @Test
    void constructorWithIntMaxValueExposesIntMaxValue() {
        ParamInt param = new ParamInt(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, param.get());
    }

    // ─── set() ───────────────────────────────────────────────────────────────────

    @Test
    void setReplacesCurrentValue() {
        ParamInt param = new ParamInt(10);
        param.set(99);
        assertEquals(99, param.get());
    }

    @Test
    void setToZeroReplacesCurrentValue() {
        ParamInt param = new ParamInt(123);
        param.set(0);
        assertEquals(0, param.get());
    }

    @Test
    void setToNegativeReplacesCurrentValue() {
        ParamInt param = new ParamInt(50);
        param.set(-50);
        assertEquals(-50, param.get());
    }

    @Test
    void setToIntMinValueIsAccepted() {
        ParamInt param = new ParamInt(0);
        param.set(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, param.get());
    }

    @Test
    void setToIntMaxValueIsAccepted() {
        ParamInt param = new ParamInt(0);
        param.set(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, param.get());
    }

    @Test
    void setIsIdempotentWhenCalledTwiceWithSameValue() {
        ParamInt param = new ParamInt(0);
        param.set(7);
        param.set(7);
        assertEquals(7, param.get());
    }

    @Test
    void setOverwritesAPreviousSet() {
        ParamInt param = new ParamInt(0);
        param.set(7);
        param.set(8);
        assertEquals(8, param.get());
    }

    // ─── add() ───────────────────────────────────────────────────────────────────

    @Test
    void addPositiveDeltaIncreasesValue() {
        ParamInt param = new ParamInt(10);
        param.add(5);
        assertEquals(15, param.get());
    }

    @Test
    void addNegativeDeltaDecreasesValue() {
        ParamInt param = new ParamInt(10);
        param.add(-3);
        assertEquals(7, param.get());
    }

    @Test
    void addZeroLeavesValueUnchanged() {
        ParamInt param = new ParamInt(42);
        param.add(0);
        assertEquals(42, param.get());
    }

    @Test
    void addIsCumulativeAcrossMultipleCalls() {
        ParamInt param = new ParamInt(0);
        param.add(1);
        param.add(2);
        param.add(3);
        assertEquals(6, param.get());
    }

    @Test
    void addAndSetCanBeMixed() {
        ParamInt param = new ParamInt(10);
        param.add(5);
        param.set(100);
        param.add(-1);
        assertEquals(99, param.get());
    }

    @Test
    void addWrapsAroundOnOverflowFromMaxValue() {
        // JML contract: ensures get() == \old(get()) + delta
        // Java int arithmetic wraps; MAX_VALUE + 1 == MIN_VALUE.
        ParamInt param = new ParamInt(Integer.MAX_VALUE);
        param.add(1);
        assertEquals(Integer.MIN_VALUE, param.get());
    }

    @Test
    void addWrapsAroundOnUnderflowFromMinValue() {
        // JML contract: ensures get() == \old(get()) + delta
        // Java int arithmetic wraps; MIN_VALUE + (-1) == MAX_VALUE.
        ParamInt param = new ParamInt(Integer.MIN_VALUE);
        param.add(-1);
        assertEquals(Integer.MAX_VALUE, param.get());
    }
}