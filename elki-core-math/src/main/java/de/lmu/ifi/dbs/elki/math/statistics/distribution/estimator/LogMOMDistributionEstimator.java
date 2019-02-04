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

import net.jafama.FastMath;

/**
 * Distribution estimators that use the method of moments (MOM) in logspace,
 * i.e. that only need the statistical moments of a data set after logarithms.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <D> Distribution estimated.
 */
public interface LogMOMDistributionEstimator<D extends Distribution> extends DistributionEstimator<D> {
  /**
   * General form of the parameter estimation
   * 
   * @param moments Statistical moments
   * @param shift Shifting offset that was used
   * @return Estimated distribution
   */
  D estimateFromLogStatisticalMoments(StatisticalMoments moments, double shift);

  @Override
  default <A> D estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int len = adapter.size(data);
    double min = min(data, adapter, 0., 1e-10);
    StatisticalMoments mv = new StatisticalMoments();
    for(int i = 0; i < len; i++) {
      final double val = adapter.getDouble(data, i) - min;
      if(Double.isInfinite(val) || Double.isNaN(val) || val <= 0.) {
        continue;
      }
      mv.put(FastMath.log(val));
    }
    return estimateFromLogStatisticalMoments(mv, min);
  }

  /**
   * Utility function to find minimum and maximum values.
   * 
   * @param <A> array type
   * @param data Data array
   * @param adapter Array adapter
   * @param minmin Minimum value for minimum.
   * @return Minimum
   */
  static <A> double min(A data, NumberArrayAdapter<?, A> adapter, double minmin, double margin) {
    final int len = adapter.size(data);
    double min = adapter.getDouble(data, 0), max = min;
    for(int i = 1; i < len; i++) {
      final double val = adapter.getDouble(data, i);
      if(val < min) {
        min = val;
      }
      else if(val > max) {
        max = val;
      }
    }
    if(min > minmin) {
      return minmin;
    }
    // Add some extra margin, to not have 0s.
    return min - (max - min) * margin;
  }
}
