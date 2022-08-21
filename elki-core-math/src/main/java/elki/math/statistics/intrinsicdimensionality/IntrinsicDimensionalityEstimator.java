/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.math.statistics.intrinsicdimensionality;

import elki.database.ids.DBIDRef;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;

/**
 * Estimate the intrinsic dimensionality from a distance list.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @param <O> Input object type when not used with distances.
 */
public interface IntrinsicDimensionalityEstimator<O> {
  /**
   * Estimate from a Reference Point, a KNNSearcher and the neighborhood size k.
   * 
   * @param knnq KNNSearcher
   * @param distq Distance query for additional distances
   * @param cur reference point
   * @param k neighborhood size
   * @return Estimated intrinsic dimensionality
   */
  double estimate(KNNSearcher<DBIDRef> knnq, DistanceQuery<? extends O> distq, DBIDRef cur, int k);

  /**
   * Estimate from a distance list.
   * 
   * @param rnq RangeSearcher
   * @param distq Distance query for additional distances
   * @param cur reference point
   * @param range neighborhood radius
   * @return Estimated intrinsic dimensionality
   */
  double estimate(RangeSearcher<DBIDRef> rnq, DistanceQuery<? extends O> distq, DBIDRef cur, double range);
}
