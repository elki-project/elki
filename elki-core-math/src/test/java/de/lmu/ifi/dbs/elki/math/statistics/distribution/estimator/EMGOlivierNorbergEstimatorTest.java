/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.statistics.distribution.ExponentiallyModifiedGaussianDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the Olivier Norberg estimation for the EMG distribution.
 * 
 * Note: this estimator works only for positively skewed data?
 * 
 * @author Erich Schubert
 */
public class EMGOlivierNorbergEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final EMGOlivierNorbergEstimator est = instantiate(EMGOlivierNorbergEstimator.class, ExponentiallyModifiedGaussianDistribution.class);
    ExponentiallyModifiedGaussianDistribution gen = new ExponentiallyModifiedGaussianDistribution(1, 2, .1, new Random(0L));
    double[] data = new double[10000];
    for(int i = 0; i < data.length; i++) {
      data[i] = gen.nextRandom();
    }
    ExponentiallyModifiedGaussianDistribution dist;
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mean", dist.getMean(), 1, -0.026544709107593434);
    assertStat("stddev", dist.getStddev(), 2, 0.17902812796064094);
    assertStat("lambda", dist.getLambda(), .1, -0.0010032987771384089);
  }
}
