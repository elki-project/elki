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
 * Unit test {@link MeanVariance}.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class MeanVarianceTest {
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
    MeanVariance mv = new MeanVariance();
    MeanVariance mc = new MeanVariance();

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
      for(int j = i + 1 - WINDOWSIZE; j <= i; j++) {
        mc.put(data[j]);
      }
      // Fully manual statistics, exact two-pass algorithm:
      double mean = 0.0;
      for(int j = i + 1 - WINDOWSIZE; j <= i; j++) {
        mean += data[j];
      }
      mean /= WINDOWSIZE;
      double var = 0.0;
      for(int j = i + 1 - WINDOWSIZE; j <= i; j++) {
        double v = data[j] - mean;
        var += v * v;
      }
      var /= (WINDOWSIZE - 1);
      assertEquals("Variance does not agree at i=" + i, mv.getSampleVariance(), mc.getSampleVariance(), 1e-14);
      assertEquals("Variance does not agree at i=" + i, mv.getSampleVariance(), var, 1e-14);
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
    MeanVariance mv = new MeanVariance();
    mv.put(evildata);
    // Values will not be exactly zero, because 1000.0001 is not exactly
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
    MeanVariance mv = new MeanVariance();
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
    MeanVariance m1 = new MeanVariance(), m2 = new MeanVariance();
    m1.put(new double[] { 1, 2, 3 });
    m2.put(new double[] { 4, 5, 6, 7 });
    MeanVariance m3 = new MeanVariance(m1);
    m3.put(m2);
    assertEquals("First mean", 2, m1.getMean(), 0.);
    assertEquals("First std", 1, m1.getSampleStddev(), 0.);
    assertEquals("Second mean", 5.5, m2.getMean(), 0.);
    assertEquals("Second std", Math.sqrt(1.25), m2.getNaiveStddev(), 0.);
    assertEquals("Third mean", 4, m3.getMean(), 0.);
    assertEquals("Third std", 4., m3.getNaiveVariance(), 0.);
    m2.put(new double[] { 1, 2, 3 }, new double[] { 4, 2, 1 });
    assertEquals("Fourth mean", 3.0, m2.getMean(), 0);
    assertEquals("Fourth weight", 11, m2.getCount(), 0);
    assertEquals("Fourth stddev", 4.8, m2.getSampleVariance(), 0);
  }

  @Test(expected = ArithmeticException.class)
  public void testEmpty() {
    new MeanVariance().put(new double[0]).getSampleVariance();
  }
}
