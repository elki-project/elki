package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Abstract base class for estimators based on the median and MAD.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <D> Distribution to generate.
 */
public abstract class AbstractLogMADEstimator<D extends Distribution> implements LogMADDistributionEstimator<D> {
  /**
   * Constructor.
   */
  public AbstractLogMADEstimator() {
    super();
  }

  @Override
  public abstract D estimateFromLogMedianMAD(double median, double mad, double shift);

  @Override
  public <A> D estimate(A data, NumberArrayAdapter<?, A> adapter) {
    // TODO: detect pre-sorted data?
    final int len = adapter.size(data);
    double min = AbstractLogMOMEstimator.min(data, adapter, 0., 1e-10);
    // Modifiable copy:
    double[] x = new double[len];
    for (int i = 0; i < len; i++) {
      final double val = adapter.getDouble(data, i) - min;
      x[i] = val > 0. ? Math.log(val) : Double.NEGATIVE_INFINITY;
      if (Double.isNaN(x[i])) {
        throw new ArithmeticException("NaN value.");
      }
    }
    double median = QuickSelect.median(x);
    double mad = computeMAD(x, median);
    return estimateFromLogMedianMAD(median, mad, min);
  }

  /**
   * Compute the median absolute deviation from median.
   * 
   * @param x Input data <b>will be modified</b>
   * @param median Median value.
   * @return Median absolute deviation from median.
   */
  public static double computeMAD(double[] x, double median) {
    // Compute deviations:
    for (int i = 0; i < x.length; i++) {
      x[i] = Math.abs(x[i] - median);
    }
    double mad = QuickSelect.median(x);
    // Fallback if we have more than 50% ties to next largest.
    if (!(mad > 0.)) {
      double min = Double.POSITIVE_INFINITY;
      for (double xi : x) {
        if (xi > 0. && xi < min) {
          min = xi;
        }
      }
      if (min < Double.POSITIVE_INFINITY) {
        mad = min;
      } else {
        mad = 1.0; // Maybe all constant. No real value.
      }
    }
    return mad;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }
}
