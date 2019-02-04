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
package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract local model neighborhood predicate.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @has - - - Instance
 * 
 * @param <O> object type
 * @param <M> model type
 * @param <N> neighborhood type
 */
public abstract class AbstractRangeQueryNeighborPredicate<O, M, N> implements NeighborPredicate<N> {
  /**
   * Range to query with.
   */
  protected double epsilon;

  /**
   * Distance function to use.
   */
  protected DistanceFunction<? super O> distFunc;

  /**
   * Full constructor.
   * 
   * @param epsilon Epsilon value
   * @param distFunc Distance function to use
   */
  public AbstractRangeQueryNeighborPredicate(double epsilon, DistanceFunction<? super O> distFunc) {
    super();
    this.epsilon = epsilon;
    this.distFunc = distFunc;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return distFunc.getInputTypeRestriction();
  }

  /**
   * Perform the preprocessing step.
   * 
   * @param modelcls Class of models
   * @param relation Data relation
   * @param query Range query
   * @return Precomputed models
   */
  public DataStore<M> preprocess(Class<? super M> modelcls, Relation<O> relation, RangeQuery<O> query) {
    WritableDataStore<M> storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, modelcls);

    Duration time = getLogger().newDuration(this.getClass().getName() + ".preprocessing-time").begin();
    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress(this.getClass().getName(), relation.size(), getLogger()) : null;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DoubleDBIDList neighbors = query.getRangeForDBID(iditer, epsilon);
      storage.put(iditer, computeLocalModel(iditer, neighbors, relation));
      getLogger().incrementProcessed(progress);
    }
    getLogger().ensureCompleted(progress);
    getLogger().statistics(time.end());
    return storage;
  }

  /**
   * Method to compute the actual data model.
   * 
   * @param id Object ID
   * @param neighbors Neighbors
   * @param relation Data relation
   * @return Model for this object.
   */
  abstract protected M computeLocalModel(DBIDRef id, DoubleDBIDList neighbors, Relation<O> relation);

  /**
   * Get the class logger.
   * 
   * @return Logger
   */
  abstract Logging getLogger();

  /**
   * Instance for a particular data set.
   * 
   * @author Erich Schubert
   * 
   * @param <N> Neighborhood type
   * @param <M> model type
   */
  public abstract static class Instance<N, M> implements NeighborPredicate.Instance<N> {
    /**
     * DBIDs to process
     */
    protected DBIDs ids;

    /**
     * Model storage.
     */
    protected DataStore<M> storage;

    /**
     * Constructor.
     * 
     * @param ids DBIDs to process
     * @param storage Model storage
     */
    public Instance(DBIDs ids, DataStore<M> storage) {
      super();
      this.ids = ids;
      this.storage = storage;
    }

    @Override
    public DBIDs getIDs() {
      return ids;
    }
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @hidden
   * 
   * @param <O> object type
   */
  public abstract static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Range to query with
     */
    double epsilon;

    /**
     * Distance function to use
     */
    DistanceFunction<O> distfun = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configDistance(config);
      configEpsilon(config);
    }

    /**
     * Configure the distance parameter.
     * 
     * @param config Parameter source
     */
    protected void configDistance(Parameterization config) {
      // Get a distance function.
      ObjectParameter<DistanceFunction<O>> distanceP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceP)) {
        distfun = distanceP.instantiateClass(config);
      }
    }

    /**
     * Configure the epsilon parameter.
     * 
     * @param config Parameter source
     */
    protected void configEpsilon(Parameterization config) {
      // Get the epsilon parameter
      DoubleParameter epsilonP = new DoubleParameter(DBSCAN.Parameterizer.EPSILON_ID);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.getValue();
      }
    }
  }
}
