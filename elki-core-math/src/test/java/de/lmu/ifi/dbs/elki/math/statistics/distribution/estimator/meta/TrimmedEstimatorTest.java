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
package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.meta;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.NormalMOMEstimator;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test for trimmed estimators.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class TrimmedEstimatorTest {
  @Test
  public void testNormalDistribution() {
    final double trim = .01;
    NormalMOMEstimator mom = NormalMOMEstimator.STATIC;
    // We could instantiate directly, but we also want to cover the
    // parameterizer class.
    ListParameterization config = new ListParameterization();
    config.addParameter(TrimmedEstimator.Parameterizer.INNER_ID, mom);
    config.addParameter(TrimmedEstimator.Parameterizer.TRIM_ID, trim);
    TrimmedEstimator<NormalDistribution> est = ClassGenericsUtil.parameterizeOrAbort(TrimmedEstimator.class, config);

    Random r = new Random(0L);
    double[] data = new double[10000];
    final int corrupt = (int) Math.floor(data.length * trim * .5);
    for(int i = 0; i < data.length; i++) {
      data[i] = i < corrupt ? 1e10 : r.nextGaussian();
    }

    NormalDistribution bad = mom.estimate(data, DoubleArrayAdapter.STATIC);
    NormalDistribution good = est.estimate(data, DoubleArrayAdapter.STATIC);
    assertEquals("Mean not as expected from naive estimator.", 5e7, bad.getMean(), 1e-2);
    assertEquals("Stddev not as expected from naive estimator.", 7e8, bad.getStddev(), 1e7);
    assertEquals("Mean not as expected from trimmed estimator.", 0, good.getMean(), 1e-2);
    assertEquals("Stddev not as expected from trimmed estimator.", 1.0, good.getStddev(), 3e-2);
  }
}
