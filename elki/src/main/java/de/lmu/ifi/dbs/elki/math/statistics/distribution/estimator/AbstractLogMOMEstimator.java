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

import de.lmu.ifi.dbs.elki.math.StatisticalMoments;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Abstract base class for estimators based on the statistical moments.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <D> Distribution to generate.
 */
public abstract class AbstractLogMOMEstimator<D extends Distribution> implements LogMOMDistributionEstimator<D> {
  /**
   * Constructor.
   */
  public AbstractLogMOMEstimator() {
    super();
  }

  @Override
  public abstract D estimateFromLogStatisticalMoments(StatisticalMoments moments, double shift);

  @Override
  public <A> D estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int len = adapter.size(data);
    double min = AbstractLogMOMEstimator.min(data, adapter, 0., 1e-10);
    StatisticalMoments mv = new StatisticalMoments();
    for (int i = 0; i < len; i++) {
      final double val = adapter.getDouble(data, i) - min;
      if (Double.isInfinite(val) || Double.isNaN(val) || val <= 0.) {
        continue;
      }
      mv.put(Math.log(val));
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
  public static <A> double min(A data, NumberArrayAdapter<?, A> adapter, double minmin, double margin) {
    final int len = adapter.size(data);
    double min = adapter.getDouble(data, 0), max = min;
    for (int i = 1; i < len; i++) {
      final double val = adapter.getDouble(data, i);
      if (val < min) {
        min = val;
      } else if (val > max) {
        max = val;
      }
    }
    if (min > minmin) {
      return minmin;
    }
    // Add some extra margin, to not have 0s.
    return min - (max - min) * margin;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }
}
