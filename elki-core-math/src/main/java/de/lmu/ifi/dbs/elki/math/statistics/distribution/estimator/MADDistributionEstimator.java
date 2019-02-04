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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Distribuition estimators that use the method of moments (MOM), i.e. that only
 * need the statistical moments of a data set.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <D> Distribution estimated.
 */
public interface MADDistributionEstimator<D extends Distribution> extends DistributionEstimator<D> {
  /**
   * General form of the parameter estimation
   * 
   * @param median Median value
   * @param mad Median absolute deviation from median
   * @return Estimated distribution
   */
  D estimateFromMedianMAD(double median, double mad);

  @Override
  default <A> D estimate(A data, NumberArrayAdapter<?, A> adapter) {
    // TODO: detect pre-sorted data?
    final int len = adapter.size(data);
    // Modifiable copy:
    double[] x = new double[len];
    for(int i = 0; i < len; i++) {
      x[i] = adapter.getDouble(data, i);
    }
    double median = QuickSelect.median(x);
    double mad = computeMAD(x, x.length, median);
    return estimateFromMedianMAD(median, mad);
  }

  /**
   * Compute the median absolute deviation from median.
   * 
   * @param data Input data
   * @param len Length of input data to use
   * @param median Median of input data
   * @param scratch Scratch space, must be at least length len
   * @return MAD
   */
  static double computeMAD(double[] data, final int len, double median, double[] scratch) {
    // Compute MAD:
    for(int i = 0; i < len; i++) {
      scratch[i] = Math.abs(data[i] - median);
    }
    double mad = QuickSelect.median(scratch);
    // Adjust MAD if 0:
    if(!(mad > 0.)) {
      double min = Double.POSITIVE_INFINITY;
      for(int i = (len >> 1); i < len; i++) {
        min = (scratch[i] > 0. && scratch[i] < min) ? scratch[i] : min;
      }
      mad = (min < Double.POSITIVE_INFINITY) ? min : 1.0;
    }
    return mad;
  }

  /**
   * Compute the median absolute deviation from median.
   * 
   * @param x Input data <b>will be modified</b>
   * @param len Length where x is valid
   * @param median Median value.
   * @return Median absolute deviation from median.
   */
  static double computeMAD(double[] x, int len, double median) {
    // Compute deviations:
    for(int i = 0; i < len; i++) {
      x[i] = Math.abs(x[i] - median);
    }
    double mad = QuickSelect.median(x);
    // Fallback if we have more than 50% ties to next largest.
    if(!(mad > 0.)) {
      double min = Double.POSITIVE_INFINITY;
      for(int i = 0; i < len; i++) {
        double xi = x[i];
        min = (xi > 0. && xi < min) ? xi : min;
      }
      // Maybe all constant. No real value.
      mad = (min < Double.POSITIVE_INFINITY) ? min : 1.0;
    }
    return mad;
  }
}
