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
public abstract class AbstractMOMEstimator<D extends Distribution> implements MOMDistributionEstimator<D> {
  /**
   * Constructor.
   */
  public AbstractMOMEstimator() {
    super();
  }

  @Override
  public abstract D estimateFromStatisticalMoments(StatisticalMoments moments);

  @Override
  public <A> D estimate(A data, NumberArrayAdapter<?, A> adapter) {
    StatisticalMoments mv = new StatisticalMoments();
    int size = adapter.size(data);
    for (int i = 0; i < size; i++) {
      final double val = adapter.getDouble(data, i);
      if (Double.isInfinite(val) || Double.isNaN(val)) {
        continue;
      }
      mv.put(val);
    }
    return estimateFromStatisticalMoments(mv);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }
}
