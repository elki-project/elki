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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.UniformDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Regression test the estimation for the UniformLMM distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class UniformLMMEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final UniformLMMEstimator est = instantiate(UniformLMMEstimator.class, UniformDistribution.class);
    load("unif.ascii.gz");
    double[] data = this.data.get("random_0_1");
    UniformDistribution dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("min", dist.getMin(), 0., -0.060238890000330725);
    assertStat("max", dist.getMax(), 1., -0.058692258199666725);
    data = this.data.get("random_M1_2");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("min", dist.getMin(), -1., 0.10386524890401616);
    assertStat("max", dist.getMax(), 2., -0.009881863067372043);
  }
}
