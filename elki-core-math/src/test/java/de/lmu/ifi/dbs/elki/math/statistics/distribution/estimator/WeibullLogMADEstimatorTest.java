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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.WeibullDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the WeibullLogMAD distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class WeibullLogMADEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final WeibullLogMADEstimator est = instantiate(WeibullLogMADEstimator.class, WeibullDistribution.class);
    load("weibull.ascii.gz");
    double[] data;
    WeibullDistribution dist;
    data = this.data.get("random_01_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.03132733662170675);
    assertStat("lambda", dist.getLambda(), 1., -0.52933465431451);
    data = this.data.get("random_01_4");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.012560010411994879);
    assertStat("lambda", dist.getLambda(), 4., 3.958760769380488);
    data = this.data.get("random_01_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.012333733709696731);
    assertStat("lambda", dist.getLambda(), 10., 0.18364326420368648);
    data = this.data.get("random_01_20");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.0073253518921420485);
    assertStat("lambda", dist.getLambda(), 20., -0.557340488161433);
    data = this.data.get("random_1_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 1., 0.15827710827809538);
    assertStat("lambda", dist.getLambda(), 1., 0.07657014745958168);
    data = this.data.get("random_2_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 2., -0.25068707686503866);
    assertStat("lambda", dist.getLambda(), 1., -0.050003950386535934);
    data = this.data.get("random_4_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., 0.0030360599354466444);
    assertStat("lambda", dist.getLambda(), 1., 0.009887216254278686);
    data = this.data.get("random_4_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., 0.0326074797911966);
    assertStat("lambda", dist.getLambda(), 10., -0.15342059253720386);
  }
}
