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
 * Regression test the estimation for the ExponentialMOM distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ExponentialMOMEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final ExponentialMOMEstimator est = instantiate(ExponentialMOMEstimator.class, ExponentialDistribution.class);
    load("exp.ascii.gz");
    double[] data = this.data.get("random_01");
    ExponentialDistribution dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 0.1, 0.012507418606315018);
    assertStat("location", dist.getLocation(), 0., 0.);
    data = this.data.get("random_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 0.5, -0.05955846076505317);
    assertStat("location", dist.getLocation(), 0., 0.);
    data = this.data.get("random_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 1., -0.08502389024636592);
    assertStat("location", dist.getLocation(), 0., 0.);
    data = this.data.get("random_2");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 2., 0.5818326126113638);
    assertStat("location", dist.getLocation(), 0., 0.);
    data = this.data.get("random_4");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 4., -0.47533646035518373);
    assertStat("location", dist.getLocation(), 0., 0.);
  }
}
