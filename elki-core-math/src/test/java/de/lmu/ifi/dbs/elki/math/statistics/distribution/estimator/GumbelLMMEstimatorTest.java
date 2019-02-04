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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.GumbelDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the GumbelLMM distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class GumbelLMMEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final GumbelLMMEstimator est = instantiate(GumbelLMMEstimator.class, GumbelDistribution.class);
    load("gumbel.ascii.gz");
    double[] data;
    GumbelDistribution dist;
    data = this.data.get("random_01_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 0.1, 0.182483158846056);
    assertStat("beta", dist.getBeta(), 1., 0.1659770406481691);
    data = this.data.get("random_01_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 0.1, 0.06763085001352262);
    assertStat("beta", dist.getBeta(), 10., -1.0006472634819428);
    data = this.data.get("random_01_20");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 0.1, 0.17314646856118152);
    assertStat("beta", dist.getBeta(), 20., -2.1664979165187646);
    data = this.data.get("random_01_4");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 0.1, 0.4259137193992216);
    assertStat("beta", dist.getBeta(), 4., 0.15954896428647292);
    data = this.data.get("random_1_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 1., 0.15272148038240707);
    assertStat("beta", dist.getBeta(), 1., -0.03259360507928721);
    data = this.data.get("random_2_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 2., 0.32334447603855976);
    assertStat("beta", dist.getBeta(), 1., 0.14797487551226984);
    data = this.data.get("random_4_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 4., 0.3063993434301695);
    assertStat("beta", dist.getBeta(), 1., 0.04974030176765987);
    data = this.data.get("random_4_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 4., 0.3909574520843453);
    assertStat("beta", dist.getBeta(), 10., -0.4383350388336691);
  }
}
