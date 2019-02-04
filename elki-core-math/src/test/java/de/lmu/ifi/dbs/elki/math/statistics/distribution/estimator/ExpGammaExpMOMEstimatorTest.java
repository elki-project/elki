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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.ExpGammaDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the ExpGamma distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ExpGammaExpMOMEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final ExpGammaExpMOMEstimator est = instantiate(ExpGammaExpMOMEstimator.class, ExpGammaDistribution.class);
    load("expgamma.ascii.gz");
    double[] data;
    ExpGammaDistribution dist;
    data = this.data.get("random_01_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, -0.03412970486951658);
    assertStat("theta", dist.getTheta(), 1., -0.282716566123276);
    data = this.data.get("random_01_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 2.370180879366545);
    assertStat("theta", dist.getTheta(), 10., -4.745500786275086);
    data = this.data.get("random_01_20");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 6.013153260552569);
    assertStat("theta", dist.getTheta(), 20., -10.48704802237117);
    data = this.data.get("random_01_4");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.49662768131324553);
    assertStat("theta", dist.getTheta(), 4., -1.4065830973895164);
    data = this.data.get("random_1_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 1., -0.21453773995576741);
    assertStat("theta", dist.getTheta(), 1., -0.23141578952065323);
    data = this.data.get("random_2_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 2., -0.6004709437458486);
    assertStat("theta", dist.getTheta(), 1., -0.2610776331914565);
    data = this.data.get("random_4_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., -0.6180258834454699);
    assertStat("theta", dist.getTheta(), 1., -0.16381753212474282);
    // Wow, this last one is really bad.
    data = this.data.get("random_4_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., 358.05349502266375);
    assertStat("theta", dist.getTheta(), 10., 308.6026295419224);
  }
}
