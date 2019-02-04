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
 * Regression test the estimation for the ExponentialMAD distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ExponentialMADEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final ExponentialMADEstimator est = instantiate(ExponentialMADEstimator.class, ExponentialDistribution.class);
    load("exp.ascii.gz");
    double[] data = this.data.get("random_01");
    ExponentialDistribution dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 0.1, 0.02137482880013364);
    assertStat("location", dist.getLocation(), 0., -0.13722743786299407);
    data = this.data.get("random_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 0.5, -0.06730155202710736);
    assertStat("location", dist.getLocation(), 0., 0.21872961956325065);
    data = this.data.get("random_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 1., -0.0016575720628470014);
    assertStat("location", dist.getLocation(), 0., 0.08118001612560888);
    data = this.data.get("random_2");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 2., 0.3750780644851286);
    assertStat("location", dist.getLocation(), 0., 0.0020751800945797427);
    data = this.data.get("random_4");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("rate", dist.getRate(), 4., -0.34649949175019046);
    assertStat("location", dist.getLocation(), 0., -0.003834216389767331);
  }
}
