/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.utilities.random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

/**
 * Unit test for our "fast non threadsafe" RNG.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class FastNonThreadsafeRandomTest {
  @Test
  public void testUniformity() {
    final int[] ranges = new int[] { 3, 6, 17, 63, 64, 1023, 1024, 49806 };
    final double[] expected = new double[] { 7.53923, 12.56330, 26.58143, 90.89696, 81.62520, 869.95303, 909.52195, 12627.10855 };
    // 0.99 expected:
    // 9.2103,15.0863,31.9999,90.8015,92.0100,1130.1073,1131.1587,50542.1598
    final int size = 10000, runs = 100;
    for(int j = 0; j < ranges.length; j++) {
      int range = ranges[j];
      int[] counts = new int[range];
      double maxchisq = 0.;
      for(int run = 0; run < runs; run++) {
        FastNonThreadsafeRandom r = new FastNonThreadsafeRandom(run);
        Arrays.fill(counts, 0);
        for(int i = 0; i < size; i++) {
          int v = r.nextInt(range);
          assertTrue("Random outside range.", v >= 0 && v < range);
          counts[v]++;
        }
        double chisq = JavaRandomTest.computeChiSquared(counts, size);
        maxchisq = chisq > maxchisq ? chisq : maxchisq;
      }
      assertEquals("Quality has changed.", expected[j], maxchisq, 1e-5);
    }
  }

  @Test
  public void testUniformityRefined() {
    final int[] ranges = new int[] { 3, 6, 17, 63, 64, 1023, 1024, 49806 };
    final double[] expected = new double[] { 7.53923, 12.56330, 26.58143, 90.89696, 81.62520, 869.95303, 909.52195, 12627.10855 };
    // 0.99 expected: 9.2103,15.0863,31.9999,90.8015,92.0100,1130.1073,1131.1587
    final int size = 10000, runs = 100;
    for(int j = 0; j < ranges.length; j++) {
      int range = ranges[j];
      int[] counts = new int[range];
      double maxchisq = 0.;
      for(int run = 0; run < runs; run++) {
        FastNonThreadsafeRandom r = new FastNonThreadsafeRandom(run);
        Arrays.fill(counts, 0);
        for(int i = 0; i < size; i++) {
          int v = r.nextIntRefined(range);
          assertTrue("Random outside range.", v >= 0 && v < range);
          counts[v]++;
        }
        double chisq = JavaRandomTest.computeChiSquared(counts, size);
        maxchisq = chisq > maxchisq ? chisq : maxchisq;
      }
      assertEquals("Quality has changed.", expected[j], maxchisq, 1e-5);
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
        Random r = new FastNonThreadsafeRandom(run);
        Arrays.fill(counts, 0);
        for(int i = 0; i < size; i++) {
          double v = r.nextDouble();
          assertTrue("Random outside range.", v >= 0 && v < 1.);
          counts[(int) Math.floor(v * range)]++;
        }
        double chisq = JavaRandomTest.computeChiSquared(counts, size);
        maxchisq = chisq > maxchisq ? chisq : maxchisq;
      }
      assertEquals("Java random quality has changed.", expected[j], maxchisq, 1e-5);
    }
  }

  @Test
  public void testSmallIntSeeds() {
    // Note: fails for seeds up to 1000, need 10000
    JavaRandomTest.assertSeedEntropy(i -> new FastNonThreadsafeRandom(i), 10000, 2, 0.35, 0.65);
    // Note: fails for seeds up to 1000, need 25000
    JavaRandomTest.assertSeedEntropy(i -> new FastNonThreadsafeRandom(i), 25000, 3, 0.29, 0.39);
    // Note: fails for seeds up to 1000, need 25000
    JavaRandomTest.assertSeedEntropy(i -> new FastNonThreadsafeRandom(i), 25000, 4, 0.20, 0.35);
    // Better way of using these:
    JavaRandomTest.assertSeedEntropy(i -> new FastNonThreadsafeRandom( //
        RandomFactory.murmurMix64(i)), 100, 2, 0.48, 0.52);
    JavaRandomTest.assertSeedEntropy(i -> new FastNonThreadsafeRandom( //
        RandomFactory.murmurMix64(i)), 100, 3, 0.32, 0.36);
    JavaRandomTest.assertSeedEntropy(i -> new FastNonThreadsafeRandom( //
        RandomFactory.murmurMix64(i)), 100, 4, 0.20, 0.31);
  }
}
