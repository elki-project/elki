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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.GeneralizedExtremeValueDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the GeneralizedExtremeValueLMM
 * distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class GeneralizedExtremeValueLMMEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final GeneralizedExtremeValueLMMEstimator est = instantiate(GeneralizedExtremeValueLMMEstimator.class, GeneralizedExtremeValueDistribution.class);
    load("gev.ascii.gz");
    GeneralizedExtremeValueDistribution dist;
    double[] data;
    data = this.data.get("random_08_02_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 0.8, -0.03040821352889378);
    assertStat("mu", dist.getMu(), 0.2, -0.0887929566709296);
    assertStat("sigma", dist.getSigma(), 1., -0.05744869446017087);
    data = this.data.get("random_1_05_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 1., -0.14332843228278025);
    assertStat("mu", dist.getMu(), 0.5, 0.03888195589591725);
    assertStat("sigma", dist.getSigma(), 0.5, -0.06223758137044988);
    data = this.data.get("random_1_05_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 1., -0.14699673138315983);
    assertStat("mu", dist.getMu(), 0.5, -0.12652497706382376);
    assertStat("sigma", dist.getSigma(), 1.0, 0.0675322107980647);
    data = this.data.get("random_2_05_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 2., -0.5871260215689651);
    assertStat("mu", dist.getMu(), 0.5, -0.07798648361068694);
    assertStat("sigma", dist.getSigma(), 0.5, -0.0029579371226723383);
    data = this.data.get("random_4_05_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), 4, -0.32202142548151347);
    assertStat("mu", dist.getMu(), 0.5, -0.018803097923999257);
    assertStat("sigma", dist.getSigma(), 0.5, 0.05765617905862008);
    // Note: for negative k, the estimation is really bad?
    data = this.data.get("random_M1_05_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), -1, 0.0884217494239612);
    assertStat("mu", dist.getMu(), 0.5, 0.03716042272425657);
    assertStat("sigma", dist.getSigma(), 0.5, 0.0952958451808682);
    data = this.data.get("random_M1_05_1");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), -1., 0.4074935910011158);
    assertStat("mu", dist.getMu(), 0.5, 0.011322871127488199);
    assertStat("sigma", dist.getSigma(), 1.0, 0.07214660181742527);
    data = this.data.get("random_M2_05_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), -2, 1.0037265433180718);
    assertStat("mu", dist.getMu(), 0.5, 0.6332110415321495);
    assertStat("sigma", dist.getSigma(), 0.5, 2.9163648075831836);
    data = this.data.get("random_M4_05_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("k", dist.getK(), -4, 3.0000007718540083);
    assertStat("mu", dist.getMu(), 0.5, 42.607935667037964);
    assertStat("sigma", dist.getSigma(), 0.5, 274.7233375224944);
  }
}
