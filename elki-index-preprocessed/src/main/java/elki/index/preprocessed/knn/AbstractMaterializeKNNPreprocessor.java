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

import elki.data.type.TypeInformation;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.DBIDRef;
import elki.database.ids.KNNList;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.knn.PreprocessorKNNQuery;
import elki.database.query.knn.PreprocessorSqrtKNNQuery;
import elki.database.query.knn.PreprocessorSquaredKNNQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.index.IndexFactory;
import elki.index.KNNIndex;
import elki.logging.Logging;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for KNN Preprocessors.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @param <O> Object type
 */
public abstract class AbstractMaterializeKNNPreprocessor<O> implements KNNIndex<O> {
  /**
   * The relation we are bound to.
   */
  protected final Relation<O> relation;

  /**
   * The query k value.
   */
  protected final int k;

  /**
   * The distance function to be used.
   */
  protected final Distance<? super O> distance;

  /**
   * The distance query we used.
   */
  protected final DistanceQuery<O> distanceQuery;

  /**
   * The data store.
   */
  protected WritableDataStore<KNNList> storage = null;

  /**
   * Constructor.
   *
   * @param relation Relation
   * @param distance Distance function
   * @param k k
   */
  public AbstractMaterializeKNNPreprocessor(Relation<O> relation, Distance<? super O> distance, int k) {
    super();
    this.k = k;
    this.relation = relation;
    this.distance = distance;
    this.distanceQuery = distance.instantiate(relation);
  }

  /**
   * Constructor.
   *
   * @param relation Relation
   * @param distanceQuery Distance query
   * @param k k
   */
  public AbstractMaterializeKNNPreprocessor(Relation<O> relation, DistanceQuery<O> distanceQuery, int k) {
    super();
    this.k = k;
    this.relation = relation;
    this.distance = distanceQuery.getDistance();
    this.distanceQuery = distanceQuery;
  }

  /**
   * The distance query we used.
   *
   * @return Distance query
   */
  public DistanceQuery<O> getDistanceQuery() {
    return distanceQuery;
  }

  /**
   * Get the value of 'k' supported by this preprocessor.
   *
   * @return k
   */
  public int getK() {
    return k;
  }

  /**
   * Perform the preprocessing step.
   */
  protected abstract void preprocess();

  /**
   * Get the k nearest neighbors.
   *
   * @param id Object ID
   * @return Neighbors
   */
  public KNNList get(DBIDRef id) {
    if(storage == null) {
      if(getLogger().isDebugging()) {
        getLogger().debug("Running kNN preprocessor: " + this.getClass());
      }
      preprocess();
    }
    return storage.get(id);
  }

  /**
   * Create the default storage.
   */
  void createStorage() {
    storage = DataStoreUtil.makeStorage(distanceQuery.getRelation().getDBIDs(), DataStoreFactory.HINT_HOT, KNNList.class);
  }

  @Override
  public void initialize() {
    if(storage != null) {
      throw new UnsupportedOperationException("Preprocessor already ran.");
    }
    if(distanceQuery.getRelation().size() > 0) {
      preprocess();
    }
  }

  /**
   * @deprecated not possible
   */
  @Deprecated
  @Override
  public KNNSearcher<O> kNNByObject(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return null; // not possible
  }

  @Override
  public PreprocessorKNNQuery kNNByDBID(DistanceQuery<O> distQ, int maxk, int flags) {
    if(relation == distQ.getRelation() && (maxk == Integer.MAX_VALUE || maxk <= k)) {
      final Distance<? super O> odist = distQ.getDistance();
      if(distance.equals(odist)) {
        return new PreprocessorKNNQuery(relation, this);
      }
      else if(EuclideanDistance.STATIC.equals(distance) && SquaredEuclideanDistance.STATIC.equals(odist)) {
        return new PreprocessorSquaredKNNQuery(relation, this);
      }
      else if(SquaredEuclideanDistance.STATIC.equals(distance) && EuclideanDistance.STATIC.equals(odist)) {
        return new PreprocessorSqrtKNNQuery(relation, this);
      }
    }
    return null;
  }

  /**
   * Get the classes static logger.
   * 
   * @return Logger
   */
  protected abstract Logging getLogger();

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
  public abstract static class Factory<O> implements IndexFactory<O> {
    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * materialized. must be an integer greater than 1.
     */
    public static final OptionID K_ID = new OptionID("materialize.k", "The number of nearest neighbors of an object to be materialized.");

    /**
     * Parameter to indicate the distance function to be used to ascertain the
     * nearest neighbors.
     */
    public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("materialize.distance", "the distance function to materialize the nearest neighbors");

    /**
     * Holds the value of {@link #K_ID}.
     */
    protected int k;

    /**
     * Hold the distance function to be used.
     */
    protected Distance<? super O> distance;

    /**
     * Index factory.
     *
     * @param k k parameter
     * @param distance distance function
     */
    public Factory(int k, Distance<? super O> distance) {
      super();
      this.k = k;
      this.distance = distance;
    }

    @Override
    public abstract AbstractMaterializeKNNPreprocessor<O> instantiate(Relation<O> relation);

    /**
     * Get the distance function.
     *
     * @return Distance function
     */
    // TODO: hide this?
    public Distance<? super O> getDistance() {
      return distance;
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return distance.getInputTypeRestriction();
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     *
     * @hidden
     *
     * @param <O> Object type
     */
    public abstract static class Par<O> implements Parameterizer {
      /**
       * Holds the value of {@link #K_ID}.
       */
      protected int k;

      /**
       * Hold the distance function to be used.
       */
      protected Distance<? super O> distance;

      @Override
      public void configure(Parameterization config) {
        new IntParameter(K_ID) //
            .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
            .grab(config, x -> k = x);
        // distance function
        new ObjectParameter<Distance<? super O>>(DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
            .grab(config, x -> distance = x);
      }

      @Override
      public abstract Factory<O> make();
    }
  }
}
