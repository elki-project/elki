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
package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Ensemble estimator taking the median of three of our best estimators.
 *
 * However, the method-of-moments estimator seems to work best at least on
 * artificial distances - you don't benefit from always choosing the second
 * best, so this ensemble approach does not appear to help.
 *
 * This is an experimental estimator. Please cite ELKI when using.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class EnsembleEstimator implements IntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final EnsembleEstimator STATIC = new EnsembleEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter, final int end) {
    double mom = MOMEstimator.STATIC.estimate(data, adapter, end);
    double mle = HillEstimator.STATIC.estimate(data, adapter, end);
    double rve = RVEstimator.STATIC.estimate(data, adapter, end);
    return (mom < mle) //
    ? (mle < rve) ? mle : //
    // A2) mom,rve < mle
    (mom < rve) ? rve : mom //
    // B) mle < mom
    : (mom < rve) ? mom : //
    // B2) mle, rve < mom
    (mle < rve) ? rve : mle;
  }
}
