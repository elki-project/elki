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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.RayleighDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the Rayleigh LMM estimation.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class RayleighLMMEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final RayleighLMMEstimator est = instantiate(RayleighLMMEstimator.class, RayleighDistribution.class);
    load("ray.ascii.gz");
    double[] data;
    RayleighDistribution dist;
    data = this.data.get("random_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("scale", dist.getSigma(), 1, -0.11722290135040081);
    data = this.data.get("random_2");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("scale", dist.getSigma(), 2, -0.002103928821441059);
  }
}
