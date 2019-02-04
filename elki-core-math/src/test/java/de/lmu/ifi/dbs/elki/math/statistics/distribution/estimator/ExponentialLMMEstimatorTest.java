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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.ExponentialDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the ExponentialLMM distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ExponentialLMMEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final ExponentialLMMEstimator est = instantiate(ExponentialLMMEstimator.class, ExponentialDistribution.class);
    load("exp.ascii.gz");
    double[] data = this.data.get("random_01");
    ExponentialDistribution dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 0.1, 0.0069924317530240115);
    assertStat("location", dist.getLocation(), 0., -0.4581527132312022);
    data = this.data.get("random_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 0.5, -0.022529011704605417);
    assertStat("location", dist.getLocation(), 0., 0.17608079496460283);
    data = this.data.get("random_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 1., -0.018315450102124098);
    assertStat("location", dist.getLocation(), 0., 0.07426754617849429);
    data = this.data.get("random_2");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 2., 0.7579698686714367);
    assertStat("location", dist.getLocation(), 0., 0.024736236642611042);
    data = this.data.get("random_4");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 4., -0.28612125430487456);
    assertStat("location", dist.getLocation(), 0., 0.014454752123533254);
  }
}
