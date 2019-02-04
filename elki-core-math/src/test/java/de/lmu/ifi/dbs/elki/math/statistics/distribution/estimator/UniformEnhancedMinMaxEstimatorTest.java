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
 * Regression test the estimation for the UniformEnhancedMinMax distribution.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class UniformEnhancedMinMaxEstimatorTest extends AbstractDistributionEstimatorTest {
  @Test
  public void testEstimator() {
    final UniformEnhancedMinMaxEstimator est = instantiate(UniformEnhancedMinMaxEstimator.class, UniformDistribution.class);
    load("unif.ascii.gz");
    double[] data = this.data.get("random_0_1");
    UniformDistribution dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("min", dist.getMin(), 0., 9.01732745378526E-4);
    assertStat("max", dist.getMax(), 1., -0.004500806282453085);
    data = this.data.get("random_M1_2");
    dist = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertStat("min", dist.getMin(), -1., 9.755307678519509E-4);
    assertStat("max", dist.getMax(), 2., -0.05135720346097461);
  }
}
