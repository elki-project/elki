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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.quality;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;

/**
 * Interface for computing the quality of a K-Means clustering.
 * <p>
 * Important note: some measures are ascending, others are descending,
 * so use the method {@link #isBetter} for ordering.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @param <O> Input Object restriction type
 */
public interface KMeansQualityMeasure<O extends NumberVector> {
  /**
   * Calculates and returns the quality measure.
   *
   * @param clustering Clustering to analyze
   * @param distanceFunction Distance function to use (usually Euclidean or
   *        squared Euclidean!)
   * @param relation Relation for accessing objects
   * @param <V> Actual vector type (could be a subtype of O!)
   *
   * @return quality measure
   */
  <V extends O> double quality(Clustering<? extends MeanModel> clustering, NumberVectorDistanceFunction<? super V> distanceFunction, Relation<V> relation);

  /**
   * Compare two scores.
   *
   * @param currentCost New (candiate) cost/score
   * @param bestCost Existing best cost/score (may be {@code NaN})
   * @return {@code true} when the new score is better, or the old score is
   *         {@code NaN}.
   */
  boolean isBetter(double currentCost, double bestCost);
}
