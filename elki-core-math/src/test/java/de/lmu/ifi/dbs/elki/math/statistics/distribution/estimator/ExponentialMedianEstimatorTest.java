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
 * Regression test the estimation for the ExponentialMedian distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ExponentialMedianEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final ExponentialMedianEstimator est = instantiate(ExponentialMedianEstimator.class, ExponentialDistribution.class);
    load("exp.ascii.gz");
    double[] data = this.data.get("random_01");
    ExponentialDistribution dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 0.1, 0.02454751899927092);
    assertStat("location", dist.getLocation(), 0., 0.);
    data = this.data.get("random_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 0.5, -0.1187374727110228);
    assertStat("location", dist.getLocation(), 0., 0.);
    data = this.data.get("random_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 1., -0.10487770515107919);
    assertStat("location", dist.getLocation(), 0., 0.);
    data = this.data.get("random_2");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 2., 0.3617817551624536);
    assertStat("location", dist.getLocation(), 0., 0.);
    data = this.data.get("random_4");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 4., -0.26561847985533316);
    assertStat("location", dist.getLocation(), 0., 0.);
  }
}
