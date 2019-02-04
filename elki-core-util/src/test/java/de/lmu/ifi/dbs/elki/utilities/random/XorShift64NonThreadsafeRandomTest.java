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
 * Unit test for our XorShift64 RNG.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class XorShift64NonThreadsafeRandomTest {
  @Test
  public void testUniformity() {
    final int[] ranges = new int[] { 3, 6, 17, 63, 64, 1023, 1024, 49806 };
    final double[] expected = new double[] { 0.434, 3.855, 15.517, 86.460, 74.437, 779.741, 806.818, 12028.688 };
    // 0.99 expected:
    // 9.2103,15.0863,31.9999,90.8015,92.0100,1130.1073,1131.1587,50542.1598
    final int size = 10000, runs = 100;
    for(int j = 0; j < ranges.length; j++) {
      int range = ranges[j];
      int[] counts = new int[range];
      double maxchisq = 0.;
      for(int run = 0; run < runs; run++) {
        XorShift64NonThreadsafeRandom r = new XorShift64NonThreadsafeRandom(run);
        Arrays.fill(counts, 0);
        for(int i = 0; i < size; i++) {
          counts[r.nextInt(range)]++;
        }
        double chisq = JavaRandomTest.computeChiSquared(counts, size);
        maxchisq = chisq > maxchisq ? chisq : maxchisq;
      }
      assertEquals("Quality has changed.", expected[j], maxchisq, 1e-3);
    }
  }


  @Test
  public void testUniformityDouble() {
    final int[] ranges = new int[] { 3, 6, 17, 63, 64, 1023, 1024, 49806 };
    final double[] expected = new double[] { 0.434, 3.855, 15.517, 86.460, 74.437, 779.741, 806.818, 12028.688 };
    // 0.99 expected:
    // 9.2103,15.0863,31.9999,90.8015,92.0100,1130.1073,1131.1587,50542.1598
    final int size = 10000, runs = 100;
    for(int j = 0; j < ranges.length; j++) {
      int range = ranges[j];
      int[] counts = new int[range];
      double maxchisq = 0.;
      for(int run = 0; run < runs; run++) {
        Random r = new XorShift64NonThreadsafeRandom(run);
        Arrays.fill(counts, 0);
        for(int i = 0; i < size; i++) {
          counts[(int) Math.floor(r.nextDouble() * range)]++;
        }
        double chisq = JavaRandomTest.computeChiSquared(counts, size);
        maxchisq = chisq > maxchisq ? chisq : maxchisq;
      }
      assertEquals("Java random quality has changed.", expected[j], maxchisq, 1e-3);
    }
  }
}
