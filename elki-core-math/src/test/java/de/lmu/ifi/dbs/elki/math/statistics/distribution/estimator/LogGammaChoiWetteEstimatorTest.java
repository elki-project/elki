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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.LogGammaDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the LogGammaChoiWette distribution.
 * 
 * @author Erich Schubert
 */
public class LogGammaChoiWetteEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final LogGammaChoiWetteEstimator est = LogGammaChoiWetteEstimator.STATIC;
    load("loggamma.ascii.gz");
    double[] data;
    LogGammaDistribution dist;
    data = this.data.get("random_01_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, Double.NaN);
    assertStat("theta", dist.getTheta(), 1., Double.NaN);
    data = this.data.get("random_01_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, Double.NaN);
    assertStat("theta", dist.getTheta(), 10., Double.NaN);
    data = this.data.get("random_01_20");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, Double.NaN);
    assertStat("theta", dist.getTheta(), 20., Double.NaN);
    data = this.data.get("random_01_4");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.1, Double.NaN);
    assertStat("theta", dist.getTheta(), 4., Double.NaN);
    data = this.data.get("random_1_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 1., Double.NaN);
    assertStat("theta", dist.getTheta(), 1., Double.NaN);
    data = this.data.get("random_2_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 2., Double.NaN);
    assertStat("theta", dist.getTheta(), 1., Double.NaN);
    data = this.data.get("random_4_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., Double.NaN);
    assertStat("theta", dist.getTheta(), 1., Double.NaN);
    data = this.data.get("random_4_10");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4., Double.NaN);
    assertStat("theta", dist.getTheta(), 10., Double.NaN);
  }
}
