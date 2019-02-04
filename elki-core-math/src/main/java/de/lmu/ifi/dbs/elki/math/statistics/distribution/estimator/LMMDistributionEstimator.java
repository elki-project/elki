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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.math.statistics.ProbabilityWeightedMoments;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Interface for distribution estimators based on the methods of L-Moments
 * (LMM).
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <D> Distribution class.
 */
public interface LMMDistributionEstimator<D extends Distribution> extends DistributionEstimator<D> {
  /**
   * Estimate from the L-Moments.
   * 
   * @param moments L-Moments
   * @return Distribution
   */
  D estimateFromLMoments(double[] moments);

  /**
   * The number of moments needed.
   * 
   * @return Moments needed.
   */
  int getNumMoments();

  @Override
  default <A> D estimate(A data, NumberArrayAdapter<?, A> adapter) {
    // Sort:
    final int size = adapter.size(data);
    double[] sorted = new double[size];
    for(int i = 0; i < size; i++) {
      sorted[i] = adapter.getDouble(data, i);
    }
    Arrays.sort(sorted);
    double[] xmom = ProbabilityWeightedMoments.samLMR(sorted, DoubleArrayAdapter.STATIC, getNumMoments());
    return estimateFromLMoments(xmom);
  }
}
