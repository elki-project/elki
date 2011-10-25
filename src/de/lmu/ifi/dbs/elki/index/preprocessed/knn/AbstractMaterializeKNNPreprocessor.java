package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.AbstractPreprocessorIndex;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for KNN Preprocessors.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public abstract class AbstractMaterializeKNNPreprocessor<O, D extends Distance<D>, T extends List<DistanceResultPair<D>>> extends AbstractPreprocessorIndex<O, T> implements KNNIndex<O> {
  /**
   * The query k value.
   */
  protected final int k;

  /**
   * The distance function to be used.
   */
  protected final DistanceFunction<? super O, D> distanceFunction;

  /**
   * The distance query we used.
   */
  protected final DistanceQuery<O, D> distanceQuery;

  /**
   * Constructor.
   * 
   * @param relation Relation
   * @param distanceFunction Distance function
   * @param k k
   */
  public AbstractMaterializeKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O, D> distanceFunction, int k) {
    super(relation);
    this.k = k;
    this.distanceFunction = distanceFunction;
    this.distanceQuery = distanceFunction.instantiate(relation);
  }

  /**
   * Get the distance factory.
   * 
   * @return distance factory
   */
  public D getDistanceFactory() {
    return distanceFunction.getDistanceFactory();
  }

  /**
   * The distance query we used.
   * 
   * @return Distance query
   */
  public DistanceQuery<O, D> getDistanceQuery() {
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
   * @param objid Object ID
   * @return Neighbors
   */
  public List<DistanceResultPair<D>> get(DBID objid) {
    if (storage == null) {
      if (getLogger().isDebugging()) {
        getLogger().debug("Running kNN preprocessor: "+this.getClass());
      }
      preprocess();
    }
    return storage.get(objid);
  }

  /**
   * Create the default storage.
   */
  void createStorage() {
    WritableDataStore<T> s = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT, List.class);
    storage = s;
  }
  
  @Override
  public void insertAll(DBIDs ids) {
    if(storage == null) {
      if(ids.size() > 0) {
        preprocess();
      }
    }
    else {
      throw new UnsupportedOperationException("Preprocessor already ran.");
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> KNNQuery<O, S> getKNNQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    getLogger().warning("Test: "+this.getClass());
    if(!this.distanceFunction.equals(distanceQuery.getDistanceFunction())) {
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
    return new PreprocessorKNNQuery<O, S, List<DistanceResultPair<S>>>(relation, (AbstractMaterializeKNNPreprocessor<O, S, List<DistanceResultPair<S>>>) this);
  }

  /**
   * The parameterizable factory.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.landmark
   * @apiviz.stereotype factory
   * @apiviz.uses AbstractMaterializeKNNPreprocessor oneway - - «create»
   * 
   * @param <O> The object type
   * @param <D> The distance type
   */
  public static abstract class Factory<O, D extends Distance<D>, T extends List<DistanceResultPair<D>>> implements IndexFactory<O, KNNIndex<O>> {
    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * materialized. must be an integer greater than 1.
     * <p>
     * Key: {@code -materialize.k}
     * </p>
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID("materialize.k", "The number of nearest neighbors of an object to be materialized.");

    /**
     * Parameter to indicate the distance function to be used to ascertain the
     * nearest neighbors.
     * <p/>
     * <p>
     * Default value: {@link EuclideanDistanceFunction}
     * </p>
     * <p>
     * Key: {@code materialize.distance}
     * </p>
     */
    public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("materialize.distance", "the distance function to materialize the nearest neighbors");

    /**
     * Holds the value of {@link #K_ID}.
     */
    protected int k;

    /**
     * Hold the distance function to be used.
     */
    protected DistanceFunction<? super O, D> distanceFunction;

    /**
     * Index factory.
     * 
     * @param k k parameter
     * @param distanceFunction distance function
     */
    public Factory(int k, DistanceFunction<? super O, D> distanceFunction) {
      super();
      this.k = k;
      this.distanceFunction = distanceFunction;
    }

    @Override
    abstract public AbstractMaterializeKNNPreprocessor<O, D, T> instantiate(Relation<O> relation);

    /**
     * Get the distance function.
     * 
     * @return Distance function
     */
    // TODO: hide this?
    public DistanceFunction<? super O, D> getDistanceFunction() {
      return distanceFunction;
    }

    /**
     * Get the distance factory.
     * 
     * @return Distance factory
     */
    public D getDistanceFactory() {
      return distanceFunction.getDistanceFactory();
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
     * @apiviz.exclude
     */
    public static abstract class Parameterizer<O, D extends Distance<D>> extends AbstractParameterizer {
      /**
       * Holds the value of {@link #K_ID}.
       */
      protected int k;

      /**
       * Hold the distance function to be used.
       */
      protected DistanceFunction<? super O, D> distanceFunction;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        // number of neighbors
        final IntParameter kP = new IntParameter(K_ID, new GreaterConstraint(1));
        if(config.grab(kP)) {
          k = kP.getValue();
        }

        // distance function
        final ObjectParameter<DistanceFunction<? super O, D>> distanceFunctionP = new ObjectParameter<DistanceFunction<? super O, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
        if(config.grab(distanceFunctionP)) {
          distanceFunction = distanceFunctionP.instantiateClass(config);
        }
      }

      @Override
      abstract protected Factory<O, D, ?> makeInstance();
    }
  }
}