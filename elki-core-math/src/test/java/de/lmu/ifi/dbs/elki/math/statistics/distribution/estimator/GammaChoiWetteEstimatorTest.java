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
 * Regression test the estimation for the GammaChoiWette distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class GammaChoiWetteEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final GammaChoiWetteEstimator est = instantiate(GammaChoiWetteEstimator.class, GammaDistribution.class);
    load("gamma.ascii.gz");
    double[] data;
    GammaDistribution dist;
    data = this.data.get("random_01_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, -0.011009099958955534);
    assertStat("theta", dist.getTheta(), 1., -0.38544240435289345);
    data = this.data.get("random_01_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, -0.006216238778006325);
    assertStat("theta", dist.getTheta(), 10., -1.6579401332478323);
    data = this.data.get("random_01_20");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, -0.009220767058099488);
    assertStat("theta", dist.getTheta(), 20., -1.7076012022695721);
    data = this.data.get("random_01_4");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, -0.007461952338469469);
    assertStat("theta", dist.getTheta(), 4., -0.19376828640721389);
    data = this.data.get("random_1_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 1., -0.11468841588478029);
    assertStat("theta", dist.getTheta(), 1., -0.06636289064185863);
    data = this.data.get("random_2_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 2., -0.40548211805937684);
    assertStat("theta", dist.getTheta(), 1., -0.2221442322212216);
    data = this.data.get("random_4_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., -0.21098492020028203);
    assertStat("theta", dist.getTheta(), 1., -0.02168065733894775);
    data = this.data.get("random_4_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., 1.0411060521829745);
    assertStat("theta", dist.getTheta(), 10., 2.3178722880996556);
  }
}
