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

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the L-M estimation for the normal distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class NormalLevenbergMarquardtKDEEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final NormalLevenbergMarquardtKDEEstimator est = instantiate(NormalLevenbergMarquardtKDEEstimator.class, NormalDistribution.class);
    load("norm.ascii.gz");
    double[] data = this.data.get("random_01_01");
    NormalDistribution dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mean", dist.getMean(), 0.1, -0.01151765753759501);
    assertStat("stddev", dist.getStddev(), 0.1, -0.009054202061335156);
    data = this.data.get("random_0_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mean", dist.getMean(), 0., 0.048356873915244764);
    assertStat("stddev", dist.getStddev(), 1., 0.0812973063260789);
    data = this.data.get("random_1_3");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mean", dist.getMean(), 1., -0.5780094891472563);
    assertStat("stddev", dist.getStddev(), 3., 0.7945863404180376);
  }
}
