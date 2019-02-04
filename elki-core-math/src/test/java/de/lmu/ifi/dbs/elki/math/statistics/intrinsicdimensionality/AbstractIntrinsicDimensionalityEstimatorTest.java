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
package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Random;

/**
 * Abstract base for testing intrinsic dimensionality estimators.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public abstract class AbstractIntrinsicDimensionalityEstimatorTest {
  /**
   * Regression test an estimator both with zeros and without.
   * 
   * @param est Estimator to test
   * @param dim Dimensionality to simulate
   * @param size Data size
   * @param seed Random seed
   * @param edim Dimensionality the estimator is known to return.
   */
  protected static void regressionTest(IntrinsicDimensionalityEstimator est, int dim, int size, long seed, double edim) {
    Random r = new Random(seed);
    final int zeros = 100;
    double[] data = new double[size + zeros];
    final double p = 1. / dim;
    for(int i = 0; i < size; i++) {
      data[i] = Math.pow(r.nextDouble(), p);
    }
    Arrays.sort(data);
    assertEquals("Accuracy of " + est.getClass().getSimpleName(), edim, est.estimate(Arrays.copyOfRange(data, zeros, data.length)), 1e-8);
    assertEquals("Accuracy of " + est.getClass().getSimpleName(), edim, est.estimate(data), 1e-8);
  }

  /**
   * Test with all zero data.
   *
   * @param est Estimator.
   */
  protected static void testZeros(IntrinsicDimensionalityEstimator est) {
    est.estimate(new double[] { 0., 0., 0., 0. });
    est.estimate(new double[] { 0., 0., 0., 1. });
    est.estimate(new double[] { 0., 0., 0., 1., 2. });
  }
}
