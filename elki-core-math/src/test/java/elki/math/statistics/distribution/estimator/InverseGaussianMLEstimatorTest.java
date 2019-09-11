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

import elki.math.statistics.distribution.InverseGaussianDistribution;
import elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the InverseGaussianML distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class InverseGaussianMLEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final InverseGaussianMLEstimator est = instantiate(InverseGaussianMLEstimator.class, InverseGaussianDistribution.class);
    load("invgauss.ascii.gz");
    double[] data = this.data.get("random_05_1");
    InverseGaussianDistribution dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mean", dist.getMean(), .5, 0.053909319141981715);
    assertStat("shape", dist.getShape(), 1., 0.2850787343985277);
    data = this.data.get("random_1_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mean", dist.getMean(), 1., -0.3048637411421484);
    assertStat("shape", dist.getShape(), .5, 0.050821602201546634);
    data = this.data.get("random_1_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mean", dist.getMean(), 1., -0.006181988682512851);
    assertStat("shape", dist.getShape(), 1., -0.060912041098003966);
  }
}
