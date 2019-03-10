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
package elki.database.query.distance;

import elki.data.spatial.SpatialComparable;
import elki.database.ids.DBIDRef;
import elki.database.query.DistanceSimilarityQuery;
import elki.database.relation.Relation;
import elki.distance.SpatialPrimitiveDistance;
import elki.similarity.PrimitiveSimilarity;

/**
 * Combination query class, to allow combined implementations of spatial
 * distances and similarities.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 * 
 * @depend - - - PrimitiveSimilarity
 * 
 * @param <O> Object type
 */
public class SpatialPrimitiveDistanceSimilarityQuery<O extends SpatialComparable> extends SpatialPrimitiveDistanceQuery<O> implements DistanceSimilarityQuery<O> {
  /**
   * Typed reference to the similarity function (usually the same as the
   * distance function!)
   */
  private PrimitiveSimilarity<? super O> similarityFunction;

  /**
   * Constructor.
   * 
   * @param relation Representation
   * @param distanceFunction distance function
   * @param similarityFunction similarity function (usually the same as the
   *        distance function!)
   */
  public SpatialPrimitiveDistanceSimilarityQuery(Relation<? extends O> relation, SpatialPrimitiveDistance<? super O> distanceFunction, PrimitiveSimilarity<? super O> similarityFunction) {
    super(relation, distanceFunction);
    this.similarityFunction = similarityFunction;
  }

  @Override
  public final double similarity(DBIDRef id1, DBIDRef id2) {
    return similarity(relation.get(id1), relation.get(id2));
  }

  @Override
  public final double similarity(O o1, DBIDRef id2) {
    return similarity(o1, relation.get(id2));
  }

  @Override
  public final double similarity(DBIDRef id1, O o2) {
    return similarity(relation.get(id1), o2);
  }

  @Override
  public double similarity(O o1, O o2) {
    return this.similarityFunction.similarity(o1, o2);
  }

  @Override
  public PrimitiveSimilarity<? super O> getSimilarity() {
    return similarityFunction;
  }
}
