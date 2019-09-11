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
package elki.math.statistics.distribution.estimator;

import org.junit.Test;

import elki.math.statistics.distribution.LogNormalDistribution;
import elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the LogNormalLogMAD distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class LogNormalLogMADEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final LogNormalLogMADEstimator est = instantiate(LogNormalLogMADEstimator.class, LogNormalDistribution.class);
    load("lognorm.ascii.gz");
    LogNormalDistribution dist;
    double[] data;
    data = this.data.get("random_0_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("logmean", dist.getLogMean(), 0, -0.03436369913718115);
    assertStat("stddev", dist.getLogStddev(), 1, -0.07952272581038411);
    assertStat("shift", dist.getShift(), 0., 0.);
    data = this.data.get("random_01_01");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("logmean", dist.getLogMean(), 0.1, -0.01931812589596546);
    assertStat("stddev", dist.getLogStddev(), 0.1, -0.023795288575369222);
    assertStat("shift", dist.getShift(), 0., 0.);
    data = this.data.get("random_1_3");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("logmean", dist.getLogMean(), 1., 0.00920155943708334);
    assertStat("stddev", dist.getLogStddev(), 3., 0.11819829486852429);
    assertStat("shift", dist.getShift(), 0., 0.);
  }
}
