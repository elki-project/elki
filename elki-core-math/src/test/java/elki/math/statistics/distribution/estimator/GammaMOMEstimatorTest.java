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

import elki.math.statistics.distribution.GammaDistribution;
import elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the Gamma distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class GammaMOMEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final GammaMOMEstimator est = instantiate(GammaMOMEstimator.class, GammaDistribution.class);
    load("gamma.ascii.gz");
    double[] data;
    GammaDistribution dist;
    data = this.data.get("random_01_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, -0.011436882562034872);
    assertStat("theta", dist.getTheta(), 1., -0.38839660582615465);
    data = this.data.get("random_01_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.0017780713508801227);
    assertStat("theta", dist.getTheta(), 10., -0.9468467326462537);
    data = this.data.get("random_01_20");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.056857634309294774);
    assertStat("theta", dist.getTheta(), 20., 11.607475721793783);
    data = this.data.get("random_01_4");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.009782418339349536);
    assertStat("theta", dist.getTheta(), 4., 0.515519106330272);
    data = this.data.get("random_1_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 1., 0.07124310643556875);
    assertStat("theta", dist.getTheta(), 1., 0.12971787024778658);
    data = this.data.get("random_2_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 2., -0.8079041058107865);
    assertStat("theta", dist.getTheta(), 1., -0.4184582828811385);
    data = this.data.get("random_4_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., -0.2968009270431873);
    assertStat("theta", dist.getTheta(), 1., -0.04383825176287526);
    data = this.data.get("random_4_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., 0.8261654492505475);
    assertStat("theta", dist.getTheta(), 10., 1.7926679244060448);
  }
}
