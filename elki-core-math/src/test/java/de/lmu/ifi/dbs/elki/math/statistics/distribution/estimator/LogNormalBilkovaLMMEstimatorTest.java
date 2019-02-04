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
 * Regression test the estimation for the LogNormalBilkovaLMM distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class LogNormalBilkovaLMMEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final LogNormalBilkovaLMMEstimator est = instantiate(LogNormalBilkovaLMMEstimator.class, LogNormalDistribution.class);
    load("lognorm.ascii.gz");
    LogNormalDistribution dist;
    double[] data;
    data = this.data.get("random_0_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("logmean", dist.getLogMean(), 0, 0.012954431446844161);
    assertStat("stddev", dist.getLogStddev(), 1, 0.014397745529324268);
    assertStat("shift", dist.getShift(), 0., 0.019737855078490796);
    data = this.data.get("random_01_01");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("logmean", dist.getLogMean(), 0.1, -0.3350477334915902);
    assertStat("stddev", dist.getLogStddev(), 0.1, 0.028169367055539768);
    assertStat("shift", dist.getShift(), 0., 0.2975810280046288);
    data = this.data.get("random_1_3");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("logmean", dist.getLogMean(), 1., 0.7242595487466166);
    assertStat("stddev", dist.getLogStddev(), 3., -0.9483779269301524);
    assertStat("shift", dist.getShift(), 0., -1.328086501539353);
  }
}
