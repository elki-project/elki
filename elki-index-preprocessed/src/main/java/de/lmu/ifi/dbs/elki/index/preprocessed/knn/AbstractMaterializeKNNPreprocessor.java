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
package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.AbstractPreprocessorIndex;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for KNN Preprocessors.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @param <O> Object type
 */
public abstract class AbstractMaterializeKNNPreprocessor<O> extends AbstractPreprocessorIndex<O, KNNList> implements KNNIndex<O> {
  /**
   * The query k value.
   */
  protected final int k;

  /**
   * The distance function to be used.
   */
  protected final DistanceFunction<? super O> distanceFunction;

  /**
   * The distance query we used.
   */
  protected final DistanceQuery<O> distanceQuery;

  /**
   * Constructor.
   *
   * @param relation Relation
   * @param distanceFunction Distance function
   * @param k k
   */
  public AbstractMaterializeKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O> distanceFunction, int k) {
    super(relation);
    this.k = k;
    this.distanceFunction = distanceFunction;
    this.distanceQuery = distanceFunction.instantiate(relation);
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
    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT, KNNList.class);
  }

  @Override
  public void initialize() {
    if(storage != null) {
      throw new UnsupportedOperationException("Preprocessor already ran.");
    }
    if(relation.size() > 0) {
      preprocess();
    }
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distQ, Object... hints) {
    if(distQ != distanceQuery && !distanceFunction.equals(distQ.getDistanceFunction())) {
      return null;
    }
    // k max supported?
    for(Object hint : hints) {
      if(hint instanceof Integer) {
        if(((Integer) hint) > k) {
          return null;
        }
        break;
      }
    }
    return new PreprocessorKNNQuery<O>(relation, this);
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
    protected DistanceFunction<? super O> distanceFunction;

    /**
     * Index factory.
     *
     * @param k k parameter
     * @param distanceFunction distance function
     */
    public Factory(int k, DistanceFunction<? super O> distanceFunction) {
      super();
      this.k = k;
      this.distanceFunction = distanceFunction;
    }

    @Override
    public abstract AbstractMaterializeKNNPreprocessor<O> instantiate(Relation<O> relation);

    /**
     * Get the distance function.
     *
     * @return Distance function
     */
    // TODO: hide this?
    public DistanceFunction<? super O> getDistanceFunction() {
      return distanceFunction;
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return distanceFunction.getInputTypeRestriction();
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
    public abstract static class Parameterizer<O> extends AbstractParameterizer {
      /**
       * Holds the value of {@link #K_ID}.
       */
      protected int k;

      /**
       * Hold the distance function to be used.
       */
      protected DistanceFunction<? super O> distanceFunction;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        // number of neighbors
        final IntParameter kP = new IntParameter(K_ID) //
            .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
        if(config.grab(kP)) {
          k = kP.getValue();
        }

        // distance function
        final ObjectParameter<DistanceFunction<? super O>> distanceFunctionP = new ObjectParameter<>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
        if(config.grab(distanceFunctionP)) {
          distanceFunction = distanceFunctionP.instantiateClass(config);
        }
      }

      @Override
      protected abstract Factory<O> makeInstance();
    }
  }
}
