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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.GammaDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the GammaLMM distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class GammaLMMEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final GammaLMMEstimator est = instantiate(GammaLMMEstimator.class, GammaDistribution.class);
    load("gamma.ascii.gz");
    double[] data;
    GammaDistribution dist;
    data = this.data.get("random_01_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, -0.024143831016068915);
    assertStat("theta", dist.getTheta(), 1., -0.4761488556215957);
    data = this.data.get("random_01_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, -0.017232059547716963);
    assertStat("theta", dist.getTheta(), 10., -2.6377967219041096);
    data = this.data.get("random_01_20");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.011891135303938705);
    assertStat("theta", dist.getTheta(), 20., 2.5465362790672863);
    data = this.data.get("random_01_4");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, -0.016259316169011648);
    assertStat("theta", dist.getTheta(), 4., -0.5556173425954172);
    data = this.data.get("random_1_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 1., -0.031215620776744557);
    assertStat("theta", dist.getTheta(), 1., 0.021666341701912284);
    data = this.data.get("random_2_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 2., -0.5280147873830532);
    assertStat("theta", dist.getTheta(), 1., -0.2819195063992386);
    data = this.data.get("random_4_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., -0.2918957768977939);
    assertStat("theta", dist.getTheta(), 1., -0.04257174762794058);
    data = this.data.get("random_4_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., 0.7661288103063901);
    assertStat("theta", dist.getTheta(), 10., 1.6459692349784252);
  }
}
