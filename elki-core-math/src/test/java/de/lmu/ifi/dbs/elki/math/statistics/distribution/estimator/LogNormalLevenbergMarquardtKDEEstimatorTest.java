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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.LogNormalDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the LogNormalLevenbergMarquardtKDE
 * distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class LogNormalLevenbergMarquardtKDEEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final LogNormalLevenbergMarquardtKDEEstimator est = instantiate(LogNormalLevenbergMarquardtKDEEstimator.class, LogNormalDistribution.class);
    load("lognorm.ascii.gz");
    LogNormalDistribution dist;
    double[] data;
    data = this.data.get("random_0_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("logmean", dist.getLogMean(), 0, -0.04064829672787483);
    assertStat("stddev", dist.getLogStddev(), 1, 0.06714852682167516);
    assertStat("shift", dist.getShift(), 0., 0);
    data = this.data.get("random_01_01");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("logmean", dist.getLogMean(), 0.1, -0.016785891820179408);
    assertStat("stddev", dist.getLogStddev(), 0.1, -0.0012405007940175933);
    assertStat("shift", dist.getShift(), 0., 0);
    data = this.data.get("random_1_3");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("logmean", dist.getLogMean(), 1., 0.020564791063600873);
    assertStat("stddev", dist.getLogStddev(), 3., 0.42629076317933556);
    assertStat("shift", dist.getShift(), 0., 0);
  }
}
