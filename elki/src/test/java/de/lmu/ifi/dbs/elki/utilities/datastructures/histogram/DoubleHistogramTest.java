/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.utilities.datastructures.histogram;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * JUnit test to test the {@link ReplacingHistogram} class.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class DoubleHistogramTest {
  /**
   * Test that adds some data to the histogram and compares results.
   */
  @Test
  public final void testHistogram() {
    double[] initial = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
    double[] filled = { 0.0, 1.23, 4.56, 7.89, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
    double[] changed = { 0.0, 1.35, 8.01, 14.67, 9.01, 2.34, 0.0, 0.0, 0.0, 0.0 };
    double[] resized = { -1.23, 0.0, 0.0, 1.35, 8.01, 14.67, 9.01, 2.34, 0.0, 0.0, 0.0, 0.0, 0.0, -4.56 };
    DoubleHistogram hist = new DoubleHistogram(10, 0.0, 1.0);
    assertArrayEquals("Empty histogram doesn't match", initial, hist.data, 1E-15);
    hist.increment(0.15, 1.23);
    hist.increment(0.25, 4.56);
    hist.increment(0.35, 7.89);
    assertArrayEquals("Filled histogram doesn't match", filled, hist.data, 1E-15);
    hist.increment(0.15, 0.12);
    hist.increment(0.25, 3.45);
    hist.increment(0.35, 6.78);
    hist.increment(0.45, 9.01);
    hist.increment(0.50, 2.34);
    assertArrayEquals("Changed histogram doesn't match", changed, hist.data, 1E-15);
    hist.increment(-.13, -1.23);
    hist.increment(1.13, -4.56);
    assertArrayEquals("Resized histogram doesn't match", resized, hist.data, 1E-15);

    // compare results via Iterator.
    int off = 0;
    for (DoubleHistogram.Iter iter = hist.iter(); iter.valid(); iter.advance()) {
      assertEquals("Array iterator bin position", -0.15 + 0.1 * off, iter.getCenter(), 0.00001);
      assertEquals("Array iterator bin contents", resized[off], iter.getValue(), 0.00001);
      off++;
    }
  }
}
