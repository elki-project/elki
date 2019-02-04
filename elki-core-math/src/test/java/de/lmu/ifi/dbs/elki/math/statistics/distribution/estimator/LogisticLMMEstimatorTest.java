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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.LogisticDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the LogisticLMM distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class LogisticLMMEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final LogisticLMMEstimator est = instantiate(LogisticLMMEstimator.class, LogisticDistribution.class);
    load("logistic.ascii.gz");
    double[] data;
    LogisticDistribution dist;
    data = this.data.get("random_01");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("location", dist.getLocation(), .1, -0.005195757237340115);
    assertStat("scale", dist.getScale(), 1., -0.01176712896963572);
    data = this.data.get("random_05");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("location", dist.getLocation(), .5, -0.11377880883757913);
    assertStat("scale", dist.getScale(), 1., 0.034939045368453714);
  }
}
