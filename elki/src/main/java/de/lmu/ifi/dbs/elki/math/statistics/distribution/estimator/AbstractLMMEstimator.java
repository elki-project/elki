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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.math.statistics.ProbabilityWeightedMoments;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Abstract base class for L-Moments based estimators (LMM).
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <D> Distribution class.
 */
public abstract class AbstractLMMEstimator<D extends Distribution> implements LMMDistributionEstimator<D> {
  /**
   * Constructor.
   */
  public AbstractLMMEstimator() {
    super();
  }

  @Override
  public <A> D estimate(A data, NumberArrayAdapter<?, A> adapter) {
    // Sort:
    final int size = adapter.size(data);
    double[] sorted = new double[size];
    for (int i = 0; i < size; i++) {
      sorted[i] = adapter.getDouble(data, i);
    }
    Arrays.sort(sorted);
    double[] xmom = ProbabilityWeightedMoments.samLMR(sorted, ArrayLikeUtil.DOUBLEARRAYADAPTER, getNumMoments());
    return estimateFromLMoments(xmom);
  }

  @Override
  abstract public D estimateFromLMoments(double[] xmom);

  @Override
  abstract public int getNumMoments();

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }
}
