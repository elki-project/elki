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

import de.lmu.ifi.dbs.elki.math.StatisticalMoments;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Distribution estimators that use the method of moments (MOM), i.e. that only
 * need the statistical moments of a data set.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <D> Distribution estimated.
 */
public interface MOMDistributionEstimator<D extends Distribution> extends DistributionEstimator<D> {
  /**
   * General form of the parameter estimation
   * 
   * @param moments Statistical moments
   * @return Estimated distribution
   */
  D estimateFromStatisticalMoments(StatisticalMoments moments);

  @Override
  default <A> D estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int size = adapter.size(data);
    StatisticalMoments mv = new StatisticalMoments();
    for (int i = 0; i < size; i++) {
      final double val = adapter.getDouble(data, i);
      if (Double.isInfinite(val) || Double.isNaN(val)) {
        continue;
      }
      mv.put(val);
    }
    return estimateFromStatisticalMoments(mv);
  }
}
