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
package elki.math.statistics.distribution.estimator;

import org.junit.Test;

import elki.math.statistics.distribution.RayleighDistribution;
import elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the Rayleigh MLE estimation.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class RayleighMLEEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final RayleighMLEEstimator est = instantiate(RayleighMLEEstimator.class, RayleighDistribution.class);
    load("ray.ascii.gz");
    double[] data;
    RayleighDistribution dist;
    data = this.data.get("random_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("scale", dist.getSigma(), 1, -0.06502897001718022);
    data = this.data.get("random_2");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("scale", dist.getSigma(), 2, 0.0067090748612135265);
  }
}
