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
package de.lmu.ifi.dbs.elki.utilities.random;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

/**
 * Unit test that uses Java random for comparison.
 * 
 * This serves two purposes:
 * 
 * 1. we notice if the Java random generation is ever modified.
 * 
 * 2. we have a baseline of values to compare to. Our random generators should
 * not be systematically worse than Javas.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class JavaRandomTest {
  @Test
  public void testUniformity() {
    final int[] ranges = new int[] { 3, 6, 17, 63, 64, 1023, 1024, 49806 };
    final double[] expected = new double[] { 7.93463, 11.10650, 31.53751, 78.94429, 81.62520, 881.99250, 909.52195, 12638.80574 };
    // 0.99 expected:
    // 9.2103,15.0863,31.9999,90.8015,92.0100,1130.1073,1131.1587,50542.1598
    final int size = 10000, runs = 100;
    for(int j = 0; j < ranges.length; j++) {
      int range = ranges[j];
      int[] counts = new int[range];
      double maxchisq = 0.;
      for(int run = 0; run < runs; run++) {
        Random r = new Random(run);
        Arrays.fill(counts, 0);
        for(int i = 0; i < size; i++) {
          counts[r.nextInt(range)]++;
        }
        double chisq = computeChiSquared(counts, size);
        maxchisq = chisq > maxchisq ? chisq : maxchisq;
      }
      assertEquals("Java random quality has changed.", expected[j], maxchisq, 1e-5);
    }
  }

  @Test
  public void testUniformityDouble() {
    final int[] ranges = new int[] { 3, 6, 17, 63, 64, 1023, 1024, 49806 };
    final double[] expected = new double[] { 9.96742, 16.40690, 31.85042, 86.57242, 103.35600, 904.59886, 890.10177, 12738.95019 };
    // 0.99 expected:
    // 9.2103,15.0863,31.9999,90.8015,92.0100,1130.1073,1131.1587,50542.1598
    final int size = 10000, runs = 100;
    for(int j = 0; j < ranges.length; j++) {
      int range = ranges[j];
      int[] counts = new int[range];
      double maxchisq = 0.;
      for(int run = 0; run < runs; run++) {
        Random r = new Random(run);
        Arrays.fill(counts, 0);
        for(int i = 0; i < size; i++) {
          counts[(int) Math.floor(r.nextDouble() * range)]++;
        }
        double chisq = computeChiSquared(counts, size);
        maxchisq = chisq > maxchisq ? chisq : maxchisq;
      }
      assertEquals("Java random quality has changed.", expected[j], maxchisq, 1e-5);
    }
  }

  /**
   * Compute the chi squared statistic.
   * 
   * @param counts Bin counts
   * @param size Number of samples
   * @return Chi-squared statistic
   */
  protected static double computeChiSquared(int[] counts, final int size) {
    final double expect = size / (double) counts.length;
    double chisq = 0.;
    for(int i = 0; i < counts.length; i++) {
      double v = Math.max(counts[i] - expect, expect - counts[i]) - .5;
      chisq += v > 0 ? (v * v) / expect : 0.;
    }
    return chisq;
  }
}
