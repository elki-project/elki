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
 * Regression test the estimation for the LogNormalLMM distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class LogNormalLMMEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final LogNormalLMMEstimator est = instantiate(LogNormalLMMEstimator.class, LogNormalDistribution.class);
    load("lognorm.ascii.gz");
    LogNormalDistribution dist;
    double[] data;
    data = this.data.get("random_0_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("logmean", dist.getLogMean(), 0, 0.01293860693389489);
    assertStat("stddev", dist.getLogStddev(), 1, 0.014406334407070176);
    assertStat("shift", dist.getShift(), 0., 0.01974990703517232);
    data = this.data.get("random_01_01");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("logmean", dist.getLogMean(), 0.1, -0.33576293434476867);
    assertStat("stddev", dist.getLogStddev(), 0.1, 0.028259826748252476);
    assertStat("shift", dist.getShift(), 0., 0.2981416393562042);
    data = this.data.get("random_1_3");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("logmean", dist.getLogMean(), 1., 0.7277441888716416);
    assertStat("stddev", dist.getLogStddev(), 3., -0.949904959684531);
    assertStat("shift", dist.getShift(), 0., -1.3443264773354375);
  }
}
