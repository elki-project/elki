package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

import static org.junit.Assert.fail;

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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.GeneralizedExtremeValueDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the GeneralizedExtremeValueLMM
 * distribution.
 * 
 * @author Erich Schubert
 */
public class GeneralizedExtremeValueLMMEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final GeneralizedExtremeValueLMMEstimator est = GeneralizedExtremeValueLMMEstimator.STATIC;
    load("gev.ascii.gz");
    GeneralizedExtremeValueDistribution dist;
    double[] data;
    data = this.data.get("random_08_02_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 0.8, -0.015832241641324907);
    assertStat("sigma", dist.getSigma(), 0.2, -0.0074716868396216);
    assertStat("k", dist.getK(), 1., -0.02674999917978249);
    data = this.data.get("random_1_05_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 1., -0.46111804410408275);
    assertStat("sigma", dist.getSigma(), 0.5, -0.06223758137044988);
    assertStat("k", dist.getK(), 0.5, 0.35667156771721975);
    data = this.data.get("random_1_05_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 1., -0.6265249770638237);
    assertStat("sigma", dist.getSigma(), 0.5, 0.5675322107980647);
    assertStat("k", dist.getK(), 1., -0.14699673138315983);
    data = this.data.get("random_2_05_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), 2., -1.577986483610687);
    assertStat("sigma", dist.getSigma(), 0.5, -0.0029579371226723383);
    assertStat("k", dist.getK(), 0.5, 0.9128739784310349);
    // Does not converge: data = this.data.get("random_4_05_05");
    data = this.data.get("random_M1_05_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), -1., 1.5371604227242566);
    assertStat("sigma", dist.getSigma(), 0.5, 0.0952958451808682);
    assertStat("k", dist.getK(), 0.5, -1.4115782505760388);
    data = this.data.get("random_M1_05_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), -1., 1.5113228711274882);
    assertStat("sigma", dist.getSigma(), 0.5, 0.5721466018174253);
    assertStat("k", dist.getK(), 1., -1.5925064089988842);
    data = this.data.get("random_M2_05_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), -2., 3.133211041532377);
    assertStat("sigma", dist.getSigma(), 0.5, 2.9163648075831836);
    assertStat("k", dist.getK(), 0.5, -1.4962734566819282);
    data = this.data.get("random_M4_05_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("mu", dist.getMu(), -4., 47.107935547828674);
    assertStat("sigma", dist.getSigma(), 0.5, 274.7233375224945);
    assertStat("k", dist.getK(), 0.5, -1.4999992281459917);
    fail("Results are not very good. Double-check");
  }
}
