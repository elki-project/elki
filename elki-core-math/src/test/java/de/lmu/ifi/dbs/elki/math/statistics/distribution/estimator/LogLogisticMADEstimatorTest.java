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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.LogLogisticDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the MAD estimation for the LogLogistic distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class LogLogisticMADEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final LogLogisticMADEstimator est = instantiate(LogLogisticMADEstimator.class, LogLogisticDistribution.class);
    load("loglogistic.ascii.gz");
    double[] data;
    LogLogisticDistribution dist;
    data = this.data.get("random_1_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("shape", dist.getShape(), 1., -0.028686755068386027);
    assertStat("location", dist.getLocation(), 0, 0);
    assertStat("scale", dist.getScale(), 1., 0.2172156997311785);
    data = this.data.get("random_2_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("shape", dist.getShape(), .5, 0.02268047558845432);
    assertStat("location", dist.getLocation(), 0, 0);
    assertStat("scale", dist.getScale(), .5, 0.07253369091897488);
    data = this.data.get("random_05_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("shape", dist.getShape(), 2., 0.20002760413066856);
    assertStat("location", dist.getLocation(), 0, 0);
    assertStat("scale", dist.getScale(), .5, -0.06881718924170005);
  }
}
