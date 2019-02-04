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
 * Regression test the estimation for the GumbelMAD distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class GumbelMADEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final GumbelMADEstimator est = instantiate(GumbelMADEstimator.class, GumbelDistribution.class);
    load("gumbel.ascii.gz");
    double[] data;
    GumbelDistribution dist;
    data = this.data.get("random_01_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 0.1, 0.072875697286744);
    assertStat("beta", dist.getBeta(), 1., 0.06519503578316921);
    data = this.data.get("random_01_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 0.1, -1.753810868376927);
    assertStat("beta", dist.getBeta(), 10., -0.6988774171236525);
    data = this.data.get("random_01_20");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 0.1, -3.4935611087359866);
    assertStat("beta", dist.getBeta(), 20., 0.5619861104511941);
    data = this.data.get("random_01_4");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 0.1, -0.8717958903684898);
    assertStat("beta", dist.getBeta(), 4., -0.49065955083474755);
    data = this.data.get("random_1_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 1., -0.08202260130739081);
    assertStat("beta", dist.getBeta(), 1., 0.04165513448725622);
    data = this.data.get("random_2_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 2., 0.030301272055886397);
    assertStat("beta", dist.getBeta(), 1., 0.06414865857628804);
    data = this.data.get("random_4_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 4., 0.17498069414407347);
    assertStat("beta", dist.getBeta(), 1., -0.060699224521309825);
    data = this.data.get("random_4_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 4., -1.0859516828967215);
    assertStat("beta", dist.getBeta(), 10., -2.5823623892099317);
  }
}
