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

import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.StatisticalMoments;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import net.jafama.FastMath;

/**
 * Estimators that work on Mean and Variance only (i.e. the first two moments
 * only).
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <D> Distribution to estimate.
 */
public interface LogMeanVarianceEstimator<D extends Distribution> extends LogMOMDistributionEstimator<D> {
  /**
   * Estimate the distribution from mean and variance.
   * 
   * @param mv Mean and variance.
   * @param shift Shift that was applied to avoid negative values.
   * @return Distribution
   */
  D estimateFromLogMeanVariance(MeanVariance mv, double shift);

  @Override
  default D estimateFromLogStatisticalMoments(StatisticalMoments moments, double shift) {
    if(!(moments.getCount() > 1.)) {
      throw new ArithmeticException("Too small sample size to estimate variance.");
    }
    return estimateFromLogMeanVariance(moments, shift);
  }

  @Override
  default <A> D estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int len = adapter.size(data);
    double min = LogMOMDistributionEstimator.min(data, adapter, 0., 1e-10);
    MeanVariance mv = new MeanVariance();
    for(int i = 0; i < len; i++) {
      final double val = adapter.getDouble(data, i) - min;
      if(Double.isInfinite(val) || Double.isNaN(val) || val <= 0.) {
        continue;
      }
      mv.put(FastMath.log(val));
    }
    if(!(mv.getCount() > 1.)) {
      throw new ArithmeticException("Too small sample size to estimate variance.");
    }
    return estimateFromLogMeanVariance(mv, min);
  }
}
