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
package elki.similarity.cluster;

import elki.data.Clustering;
import elki.database.query.DistanceSimilarityQuery;
import elki.database.relation.Relation;
import elki.distance.PrimitiveDistance;
import elki.similarity.PrimitiveSimilarity;

/**
 * Distance and similarity measure for clusterings.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public interface ClusteringDistanceSimilarity extends PrimitiveSimilarity<Clustering<?>>, PrimitiveDistance<Clustering<?>> {
  @Override
  default boolean isSymmetric() {
    return true;
  }

  @Override
  <T extends Clustering<?>> DistanceSimilarityQuery<T> instantiate(Relation<T> relation);
}
