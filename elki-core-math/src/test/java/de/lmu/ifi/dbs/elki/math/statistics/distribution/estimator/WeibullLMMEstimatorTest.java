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
 * Regression test the estimation for the WeibullLMM distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class WeibullLMMEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final WeibullLMMEstimator est = instantiate(WeibullLMMEstimator.class, WeibullDistribution.class);
    load("weibull.ascii.gz");
    double[] data;
    WeibullDistribution dist;
    data = this.data.get("random_01_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    // FIXME: Pretty large error!
    assertStat("k", dist.getK(), 0.1, 0.09454063929803297);
    assertStat("lambda", dist.getLambda(), 1., 1200.6518885193916);
    data = this.data.get("random_01_4");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.017892872573341767);
    assertStat("lambda", dist.getLambda(), 4., -0.572620960388186);
    data = this.data.get("random_01_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.015515482254432902);
    assertStat("lambda", dist.getLambda(), 10., 30.54011957136276);
    data = this.data.get("random_01_20");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.05028759358784987);
    assertStat("lambda", dist.getLambda(), 20., 364.1538008873844);
    data = this.data.get("random_1_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 1., 0.3174145503264614);
    assertStat("lambda", dist.getLambda(), 1., 0.1373923548546876);
    data = this.data.get("random_2_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 2., -0.24025439963457784);
    assertStat("lambda", dist.getLambda(), 1., -0.09621573512109494);
    data = this.data.get("random_4_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., 1.1101681921910798);
    assertStat("lambda", dist.getLambda(), 1., 0.22152376290215847);
    data = this.data.get("random_4_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., -0.24710131145373015);
    assertStat("lambda", dist.getLambda(), 10., 0.19194686181006482);
  }
}
