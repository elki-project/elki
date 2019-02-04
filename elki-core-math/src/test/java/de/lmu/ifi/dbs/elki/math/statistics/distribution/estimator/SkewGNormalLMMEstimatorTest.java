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

import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.statistics.distribution.SkewGeneralizedNormalDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the L-Moments estimation for the skewed generalized normal
 * distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class SkewGNormalLMMEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final SkewGNormalLMMEstimator est = instantiate(SkewGNormalLMMEstimator.class, SkewGeneralizedNormalDistribution.class);
    load("norm.ascii.gz");
    SkewGeneralizedNormalDistribution dist;
    double[] data;
    data = this.data.get("random_0_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("loc", dist.getLocation(), 0., 0.02039022923745077);
    assertStat("scale", dist.getScale(), 1., -0.024058107085344838);
    assertStat("skew", dist.getSkew(), 0., 0.08831652708468758);

    data = this.data.get("random_1_3");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("loc", dist.getLocation(), 1., -0.4979572257964966);
    assertStat("scale", dist.getScale(), 3., 0.23971276527848406);
    assertStat("skew", dist.getSkew(), 0., -0.04874213163111496);

    data = this.data.get("random_01_01");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("loc", dist.getLocation(), .1, -0.007991229792025226);
    assertStat("scale", dist.getScale(), .1, -0.008932536363814136);
    assertStat("skew", dist.getSkew(), 0., 0.052527462352286544);

    // FIXME: Test with skewed independent reference data!

    SkewGeneralizedNormalDistribution gen = new SkewGeneralizedNormalDistribution(6., 4., 3., new Random(0L));
    data = new double[10000];
    for(int i = 0; i < data.length; i++) {
      data[i] = gen.nextRandom();
    }
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("loc", dist.getLocation(), 6., -0.2453097710580181);
    // The scale is not estimated very well. Almost 5 rather than 4 :-(
    assertStat("scale", dist.getScale(), 4., 0.9661738796034509);
    assertStat("skew", dist.getSkew(), 3., -0.19338427549778192);
  }
}
