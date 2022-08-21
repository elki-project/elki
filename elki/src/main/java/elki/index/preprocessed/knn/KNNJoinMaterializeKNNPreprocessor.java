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
package elki.index.preprocessed.knn;

import elki.algorithm.KNNJoin;
import elki.data.spatial.SpatialComparable;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.SpatialPrimitiveDistance;
import elki.logging.Logging;

/**
 * Class to materialize the kNN using a spatial join on an R-tree.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <V> vector type
 */
public class KNNJoinMaterializeKNNPreprocessor<V extends SpatialComparable> extends AbstractMaterializeKNNPreprocessor<V> {
  /**
   * Logging class.
   */
  private static final Logging LOG = Logging.getLogger(KNNJoinMaterializeKNNPreprocessor.class);

  /**
   * Constructor.
   *
   * @param relation Relation to index
   * @param distance Distance function
   * @param k k
   */
  public KNNJoinMaterializeKNNPreprocessor(Relation<V> relation, Distance<? super V> distance, int k) {
    super(relation, distance, k);
  }

  @Override
  protected void preprocess() {
    SpatialPrimitiveDistance<?> distFunction = (SpatialPrimitiveDistance<?>) distance;
    storage = new KNNJoin(distFunction, k).run(relation, relation.getDBIDs());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  // @Override
  public String getLongName() {
    return "knn-join materialized neighbors";
  }

  // @Override
  public String getShortName() {
    return "knn-join";
  }

  /**
   * The parameterizable factory.
   *
   * @author Erich Schubert
   *
   * @opt nodefillcolor LemonChiffon
   * @stereotype factory
   * @navassoc - create - AbstractMaterializeKNNPreprocessor
   *
   * @param <O> The object type
   */
  public static class Factory<O extends SpatialComparable> extends AbstractMaterializeKNNPreprocessor.Factory<O> {
    /**
     * Constructor.
     *
     * @param k K
     * @param distance distance function
     */
    public Factory(int k, Distance<? super O> distance) {
      super(k, distance);
    }

    @Override
    public KNNJoinMaterializeKNNPreprocessor<O> instantiate(Relation<O> relation) {
      return new KNNJoinMaterializeKNNPreprocessor<>(relation, distance, k);
    }

    /**
     * Parameterization class
     *
     * @author Erich Schubert
     *
     * @hidden
     *
     * @param <O> Object type
     */
    public static class Par<O extends SpatialComparable> extends AbstractMaterializeKNNPreprocessor.Factory.Par<O> {
      @Override
      public KNNJoinMaterializeKNNPreprocessor.Factory<O> make() {
        return new KNNJoinMaterializeKNNPreprocessor.Factory<>(k, distance);
      }
    }
  }
}
