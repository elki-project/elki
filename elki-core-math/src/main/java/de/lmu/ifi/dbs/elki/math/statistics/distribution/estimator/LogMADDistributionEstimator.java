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

import net.jafama.FastMath;

/**
 * Distribuition estimators that use the method of moments (MOM) in logspace.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <D> Distribution estimated.
 */
public interface LogMADDistributionEstimator<D extends Distribution> extends DistributionEstimator<D> {
  /**
   * General form of the parameter estimation
   * 
   * @param median Median lof log values.
   * @param mad Median absolute deviation from median (in logspace).
   * @param shift Shift offset that was used to avoid negative values.
   * @return Estimated distribution
   */
  D estimateFromLogMedianMAD(double median, double mad, double shift);

  @Override
  default <A> D estimate(A data, NumberArrayAdapter<?, A> adapter) {
    // TODO: detect pre-sorted data?
    final int len = adapter.size(data);
    double min = LogMOMDistributionEstimator.min(data, adapter, 0., 1e-10);
    // Modifiable copy:
    double[] x = new double[len];
    for(int i = 0; i < len; i++) {
      final double val = adapter.getDouble(data, i) - min;
      x[i] = val > 0. ? FastMath.log(val) : Double.NEGATIVE_INFINITY;
      if(Double.isNaN(x[i])) {
        throw new ArithmeticException("NaN value.");
      }
    }
    double median = QuickSelect.median(x);
    double mad = MADDistributionEstimator.computeMAD(x, x.length, median);
    return estimateFromLogMedianMAD(median, mad, min);
  }
}
