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
package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.meta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.UniformDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.*;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Test for the best fit estimator.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class BestFitEstimatorTest {
  private BestFitEstimator init() {
    BestFitEstimator est = new BestFitEstimator();
    // In order to not "cheat" our test coverage, only use normal and lognormal
    // distribution estimators:
    est.momests = Arrays.asList(NormalMOMEstimator.STATIC);
    est.madests = Arrays.asList(NormalMADEstimator.STATIC);
    est.lmmests = Arrays.asList(NormalLMMEstimator.STATIC);
    est.logmomests = Arrays.asList(LogNormalLogMOMEstimator.STATIC);
    est.logmadests = Arrays.asList(LogNormalLogMADEstimator.STATIC);
    return est;
  }

  @Test
  public void testNormalDistribution() {
    BestFitEstimator est = init();

    Random r = new Random(0L);
    double[] data = new double[10000];
    for(int i = 0; i < data.length; i++) {
      data[i] = 2 + 3 * r.nextGaussian();
    }

    Distribution edist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertEquals("Wrong class of distribution", NormalDistribution.class, edist.getClass());
    NormalDistribution good = (NormalDistribution) edist;
    assertEquals("Mean not as expected from trimmed estimator.", 2., good.getMean(), 2e-2);
    assertEquals("Stddev not as expected from trimmed estimator.", 3., good.getStddev(), 4e-2);
  }

  @Test
  public void testConstant() {
    BestFitEstimator est = init();
    Distribution edist = est.estimate(new double[100], DoubleArrayAdapter.STATIC);
    assertEquals("Wrong class of distribution", UniformDistribution.class, edist.getClass());
  }

  @Test
  public void testExtreme() {
    BestFitEstimator est = init();
    Distribution edist = est.estimate(new double[] { Double.MAX_VALUE, -Double.MAX_VALUE }, DoubleArrayAdapter.STATIC);
    assertNotNull("Wrong class of distribution", edist);
  }

  @Test(expected = ArithmeticException.class)
  public void testEmpty() {
    BestFitEstimator est = init();
    Distribution edist = est.estimate(new double[0], DoubleArrayAdapter.STATIC);
    assertEquals("Wrong class of distribution", UniformDistribution.class, edist.getClass());
  }

  @Test(expected = ArithmeticException.class)
  public void testInf() {
    BestFitEstimator est = init();
    Distribution edist = est.estimate(new double[] { Double.POSITIVE_INFINITY }, DoubleArrayAdapter.STATIC);
    assertEquals("Wrong class of distribution", UniformDistribution.class, edist.getClass());
  }

  @Test(expected = ArithmeticException.class)
  public void testNaN() {
    BestFitEstimator est = init();
    Distribution edist = est.estimate(new double[] { Double.NaN }, DoubleArrayAdapter.STATIC);
    assertEquals("Wrong class of distribution", UniformDistribution.class, edist.getClass());
  }

  @Test
  public void testParameterizer() {
    BestFitEstimator est = AbstractDistributionEstimatorTest.instantiate(BestFitEstimator.class, Distribution.class);
    assertEquals("Not static instance", BestFitEstimator.STATIC, est);
  }
}
