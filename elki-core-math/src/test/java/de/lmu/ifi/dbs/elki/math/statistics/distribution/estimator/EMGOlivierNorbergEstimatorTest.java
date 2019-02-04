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
 * @since 0.7.5
 */
public class EMGOlivierNorbergEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final EMGOlivierNorbergEstimator est = instantiate(EMGOlivierNorbergEstimator.class, ExponentiallyModifiedGaussianDistribution.class);
    ExponentiallyModifiedGaussianDistribution dist, gen;

    gen = new ExponentiallyModifiedGaussianDistribution(1, 2, .1, new Random(0L));
    double[] data = new double[10000];
    for(int i = 0; i < data.length; i++) {
      data[i] = gen.nextRandom();
    }
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mean", dist.getMean(), 1, -0.0265447091074848);
    assertStat("stddev", dist.getStddev(), 2, 0.17902812796064094);
    assertStat("lambda", dist.getLambda(), .1, -0.0010032987771384089);

    gen = new ExponentiallyModifiedGaussianDistribution(5, 4, .3, new Random(0L));
    data = new double[10000];
    for(int i = 0; i < data.length; i++) {
      data[i] = gen.nextRandom();
    }
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mean", dist.getMean(), 5, 0.005239133140687358);
    assertStat("stddev", dist.getStddev(), 4, 0.05032298704431959);
    assertStat("lambda", dist.getLambda(), .3, 7.728347755092679E-4);

    load("emg.ascii.gz");
    data = this.data.get("random_1_3_01");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mean", dist.getMean(), 1, 0.9742484598719088);
    assertStat("stddev", dist.getStddev(), 3., 0.5404618548719853);
    assertStat("lambda", dist.getLambda(), .1, 0.018125211915103898);

    // This one is not fit correctly. :-( Too much skew? Negative L-kurtosis.
    data = this.data.get("random_1_3_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mean", dist.getMean(), 1, 2.2452576306758862);
    assertStat("stddev", dist.getStddev(), 3, 0.5522898083602992);
    assertStat("lambda", dist.getLambda(), .5, 0.5);
  }
}
