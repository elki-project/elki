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
package de.lmu.ifi.dbs.elki.math;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.squareSum;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.sum;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

/**
 * Unit test {@link MeanVarianceMinMax}.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class MeanVarianceMinMaxTest {
  /**
   * Size of test data set.
   */
  private static final int SIZE = 100000;

  /**
   * Sliding window size.
   */
  private static final int WINDOWSIZE = 100;

  @Test
  public void testSlidingWindowVariance() {
    MeanVarianceMinMax mv = new MeanVarianceMinMax();
    MeanVarianceMinMax mc = new MeanVarianceMinMax();

    Random r = new Random(0L);
    double[] data = new double[SIZE];
    for(int i = 0; i < data.length; i++) {
      data[i] = r.nextDouble();
    }
    // Arrays.sort(data);

    // Pre-roll:
    for(int i = 0; i < WINDOWSIZE; i++) {
      mv.put(data[i]);
    }
    // Compare to window approach
    for(int i = WINDOWSIZE; i < data.length; i++) {
      mv.put(data[i - WINDOWSIZE], -1.); // Remove
      mv.put(data[i]);

      mc.reset(); // Reset statistics
      double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
      for(int j = i + 1 - WINDOWSIZE; j <= i; j++) {
        final double v = data[j];
        mc.put(v);
        min = v < min ? v : min;
        max = v > max ? v : max;
      }
      // Fully manual statistics, exact two-pass algorithm:
      double mean = 0.0;
      for(int j = i + 1 - WINDOWSIZE; j <= i; j++) {
        mean += data[j];
      }
      mean /= WINDOWSIZE;
      double var = 0.0, errs = 0.0;
      for(int j = i + 1 - WINDOWSIZE; j <= i; j++) {
        double v = data[j] - mean;
        errs += v;
        var += v * v;
      }
      errs /= WINDOWSIZE;
      mean += errs;
      var /= WINDOWSIZE;
      var += errs * errs;
      assertEquals("Variance does not agree at i=" + i, mc.getSampleVariance(), mv.getSampleVariance(), 1e-14);
      assertEquals("Variance does not agree at i=" + i, var, mv.getNaiveVariance(), 1e-14);
      // We can only test mc here:
      assertEquals("Min does not agree at i=" + i, min, mc.getMin(), 0);
      assertEquals("Max does not agree at i=" + i, max, mc.getMax(), 0);
    }
  }

  /**
   * Test to disallow using E[X^2] - E[X]^2, which is numerically problematic.
   */
  @Test
  public void notNaive() {
    double[] evildata = { 1000.0001, 1000.0001, 1000.0001, 1000.0001, 1000.0001, 1000.0001 };
    // Demo the problem first:
    double estX = sum(evildata) / evildata.length;
    double estXsq = squareSum(evildata) / evildata.length;
    double badvar = estXsq - estX * estX;
    assertTrue(badvar < 0); // Variance should always be non-negative!
    MeanVarianceMinMax mv = new MeanVarianceMinMax();
    mv.put(evildata);
    // Values will not be exactly zero, because 1000.0001 is no)t exactly
    // representable as float.
    // (But that is not what is causing the problem above).
    assertEquals("Variance is not zero", 0, mv.getNaiveVariance(), 2e-14);
    assertEquals("Mean is bad", 1000.0001, mv.getMean(), 1e-12);
  }

  /**
   * Note: this test tests an earlier bug with tiny arrays. Keep.
   */
  @Test
  public void basic() {
    MeanVarianceMinMax mv = new MeanVarianceMinMax();
    mv.put(0);
    mv.put(new double[] {});
    mv.put(new double[] { 0 });
    mv.put(new double[] { 0, 0 });
    mv.put(new double[] { 0, 0, 0 });
    assertEquals("Count wrong.", 7, mv.getCount(), 0.);
    assertEquals("Mean wrong.", 0, mv.getMean(), 0.);
    assertEquals("Variance wrong.", 0, mv.getNaiveVariance(), 0.);
    assertEquals("No toString", -1, mv.toString().indexOf('@'));
  }

  @Test
  public void combine() {
    MeanVarianceMinMax m1 = new MeanVarianceMinMax(),
        m2 = new MeanVarianceMinMax();
    m1.put(new double[] { 1, 2, 3 });
    m2.put(new double[] { 4, 5, 6, 7 });
    MeanVarianceMinMax m3 = new MeanVarianceMinMax(m1);
    m3.put(m2);
    assertEquals("First mean", 2, m1.getMean(), 0.);
    assertEquals("First std", 1, m1.getSampleStddev(), 0.);
    assertEquals("First min", 1, m1.getMin(), 0.);
    assertEquals("First max", 3, m1.getMax(), 0.);
    assertEquals("First max", 2, m1.getDiff(), 0.);
    assertEquals("Second mean", 5.5, m2.getMean(), 0.);
    assertEquals("Second std", Math.sqrt(1.25), m2.getNaiveStddev(), 0.);
    assertEquals("Second min", 4, m2.getMin(), 0.);
    assertEquals("Second max", 7, m2.getMax(), 0.);
    assertEquals("Third mean", 4, m3.getMean(), 0.);
    assertEquals("Third std", 4., m3.getNaiveVariance(), 0.);
    assertEquals("Third min", 1, m3.getMin(), 0.);
    assertEquals("Third max", 7, m3.getMax(), 0.);
    m2.put(new double[] { 1, 2, 3 }, new double[] { 4, 2, 1 });
    assertEquals("Fourth mean", 3.0, m2.getMean(), 0);
    assertEquals("Fourth stddev", 4.8, m2.getSampleVariance(), 0);
    m2.put(new double[] { 0, 100, 9 }, new double[] { .01, 0, 99 });
    assertEquals("First min", 0, m2.getMin(), 0.);
    assertEquals("First max", 9, m2.getMax(), 0.);
  }
}
