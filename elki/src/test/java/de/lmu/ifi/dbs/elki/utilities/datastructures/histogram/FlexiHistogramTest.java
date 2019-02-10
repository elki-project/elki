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

import java.util.Random;

import org.junit.Test;

/**
 * JUnit test to test the {@link AbstractObjDynamicHistogram} class.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class FlexiHistogramTest {
  /**
   * Test that adds some data to the histogram and compares results.
   */
  @Test
  public final void testObjHistogram() {
    Double[] filled = { 0.0, 1.23, 4.56, 7.89, 0.0, 0.0, null, null, null, null };
    Double[] changed = { 0.0, 1.35, 8.01, 14.67, 9.01, 2.34, null, null, null, null };
    Double[] resized = { -1.23, 1.35, 22.68, 11.35, 0.0, 0.0, -4.56, null, null, null };
    Double[] expanded = { 1., 0.0, 0.0, 0.0, 0.0, 0.0, 34.15, -4.56, null, null };
    AbstractObjDynamicHistogram<Double> hist = new AbstractObjDynamicHistogram<Double>(5) {
      @Override
      public Double aggregate(Double first, Double second) {
        return Double.valueOf(first.doubleValue() + second.doubleValue());
      }

      @Override
      protected Double downsample(Object[] data, int start, int end, int size) {
        double sum = 0;
        for (int i = start; i < end; i++) {
          final Double val = (Double) data[i];
          if (val != null) {
            sum += val;
          }
        }
        return Double.valueOf(sum);
      }

      @Override
      protected Double makeObject() {
        return Double.valueOf(0.);
      }

      @Override
      protected Double cloneForCache(Double data) {
        return data;
      }
    };
    hist.putData(0.0, 0.0);
    hist.putData(0.15, 1.23);
    hist.putData(0.25, 4.56);
    hist.putData(0.35, 7.89);
    hist.putData(0.5, 0.0);
    hist.materialize();
    assertArrayEquals("Filled histogram doesn't match", filled, hist.data);
    hist.putData(0.15, 0.12);
    hist.putData(0.25, 3.45);
    hist.putData(0.35, 6.78);
    hist.putData(0.45, 9.01);
    hist.putData(0.55, 2.34);
    assertArrayEquals("Changed histogram doesn't match", changed, hist.data);
    hist.putData(-.13, -1.23);
    hist.putData(1.13, -4.56);
    assertArrayEquals("Resized histogram doesn't match", resized, hist.data);

    // compare results via Iterator.
    int off = 0;
    for (ObjHistogram<Double>.Iter iter = hist.iter(); iter.valid(); iter.advance()) {
      assertEquals("Array iterator bin position", -0.1 + 0.2 * off, iter.getCenter(), 0.00001);
      assertEquals("Array iterator bin contents", resized[off], iter.getValue(), 0.00001);
      off++;
    }

    // totally break out of the data range
    hist.putData(-10., 1.);
    assertArrayEquals("Expanded histogram doesn't match", expanded, hist.data);

    // Try some random operations, too
    Random r = new Random(0);
    for (int i = 0; i < 1000; i++) {
      hist.putData((r.nextDouble() - .5) * i * i, i * .1);
    }
  }

  /**
   * Test that adds some data to the histogram and compares results.
   */
  @Test
  public final void testDoubleHistogram() {
    double[] filled = { 0.0, 1.23, 4.56, 7.89, 0.0, 0, 0, 0, 0, 0 };
    double[] changed = { 0.0, 1.35, 8.01, 14.67, 9.01, 2.34, 0, 0, 0, 0 };
    double[] resized = { -1.23, 1.35, 22.68, 11.35, 0.0, 0.0, -4.56, 0, 0, 0 };
    double[] expanded = { 1., 0.0, 0.0, 0.0, 0.0, 0.0, 34.15, -4.56, 0, 0 };
    DoubleDynamicHistogram hist = new DoubleDynamicHistogram(5);
    hist.increment(0.0, 0.0);
    hist.increment(0.15, 1.23);
    hist.increment(0.25, 4.56);
    hist.increment(0.35, 7.89);
    hist.increment(0.5, 0.0);
    hist.materialize();
    assertArrayEquals("Filled histogram doesn't match", filled, hist.data, 1E-15);
    hist.increment(0.15, 0.12);
    hist.increment(0.25, 3.45);
    hist.increment(0.35, 6.78);
    hist.increment(0.45, 9.01);
    hist.increment(0.55, 2.34);
    assertArrayEquals("Changed histogram doesn't match", changed, hist.data, 1E-15);
    hist.increment(-.13, -1.23);
    hist.increment(1.13, -4.56);
    assertArrayEquals("Resized histogram doesn't match", resized, hist.data, 1E-15);

    // compare results via Iterator.
    int off = 0;
    for (DoubleHistogram.Iter iter = hist.iter(); iter.valid(); iter.advance()) {
      assertEquals("Array iterator bin position", -0.1 + 0.2 * off, iter.getCenter(), 0.00001);
      assertEquals("Array iterator bin contents", resized[off], iter.getValue(), 0.00001);
      off++;
    }

    // totally break out of the data range
    hist.increment(-10., 1.);
    assertArrayEquals("Expanded histogram doesn't match", expanded, hist.data, 1E-15);

    // Try some random operations, too
    Random r = new Random(0);
    for (int i = 0; i < 1000; i++) {
      hist.increment((r.nextDouble() - .5) * i * i, i * .1);
    }
  }
}
