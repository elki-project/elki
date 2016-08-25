package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.statistics.distribution.WeibullDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the WeibullLogMAD distribution.
 * 
 * @author Erich Schubert
 */
public class WeibullLogMADEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final WeibullLogMADEstimator est = WeibullLogMADEstimator.STATIC;
    load("weibull.ascii.gz");
    double[] data;
    WeibullDistribution dist;
    data = this.data.get("random_01_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.12320836574022365);
    assertStat("lambda", dist.getLambda(), 1., Double.NaN);
    data = this.data.get("random_01_4");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.0913107858429773);
    assertStat("lambda", dist.getLambda(), 4., Double.NaN);
    data = this.data.get("random_01_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.09092619833648924);
    assertStat("lambda", dist.getLambda(), 10., Double.NaN);
    data = this.data.get("random_01_20");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, 0.08241378386698991);
    assertStat("lambda", dist.getLambda(), 20., Double.NaN);
    data = this.data.get("random_1_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 1., 0.9686467955852291);
    assertStat("lambda", dist.getLambda(), 1., Double.NaN);
    data = this.data.get("random_2_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 2., 0.9731911785125611);
    assertStat("lambda", dist.getLambda(), 1., Double.NaN);
    data = this.data.get("random_4_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., 2.8036949497511463);
    assertStat("lambda", dist.getLambda(), 1., Double.NaN);
    data = this.data.get("random_4_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., 2.8539555311991123);
    assertStat("lambda", dist.getLambda(), 10., Double.NaN);
  }
}
