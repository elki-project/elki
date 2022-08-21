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
package elki.index.preprocessed.snn;

import elki.data.type.TypeInformation;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * A preprocessor for annotation of the ids of nearest neighbors to each
 * database object.
 * <p>
 * The k nearest neighbors are assigned based on an arbitrary distance function.
 * <p>
 * This functionality is similar but not identical to
 * {@link MaterializeKNNPreprocessor}: While it also computes the k nearest
 * neighbors, it does not keep the actual distances, but organizes the NN set in
 * a TreeSet for fast set operations.
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.1
 * 
 * @has - - - Distance
 * 
 * @param <O> the type of database objects the preprocessor can be applied to
 */
@Title("Shared Nearest Neighbor Preprocessor")
@Description("Computes the k nearest neighbors of objects of a certain database.")
public class SharedNearestNeighborPreprocessor<O> implements SharedNearestNeighborIndex<O> {
  /**
   * Get a logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SharedNearestNeighborPreprocessor.class);

  /**
   * Holds the number of nearest neighbors to be used.
   */
  protected int numberOfNeighbors;

  /**
   * Hold the distance function to be used.
   */
  protected Distance<O> distance;

  /**
   * The data store.
   */
  protected WritableDataStore<ArrayDBIDs> storage = null;

  /**
   * Relation to use.
   */
  protected Relation<O> relation;

  /**
   * Constructor.
   * 
   * @param relation Database to use
   * @param numberOfNeighbors Number of neighbors
   * @param distance Distance function
   */
  public SharedNearestNeighborPreprocessor(Relation<O> relation, int numberOfNeighbors, Distance<O> distance) {
    super();
    this.relation = relation;
    this.distance = distance;
    this.numberOfNeighbors = numberOfNeighbors;
  }

  @Override
  public void initialize() {
    if(LOG.isVerbose()) {
      LOG.verbose("Assigning nearest neighbor lists to database objects");
    }
    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, ArrayDBIDs.class);
    KNNSearcher<DBIDRef> knnquery = new QueryBuilder<>(relation, distance).kNNByDBID(numberOfNeighbors);

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("assigning nearest neighbor lists", relation.size(), LOG) : null;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      ArrayModifiableDBIDs neighbors = DBIDUtil.newArray(numberOfNeighbors);
      DBIDs kNN = knnquery.getKNN(iditer, numberOfNeighbors);
      for(DBIDIter iter = kNN.iter(); iter.valid(); iter.advance()) {
        neighbors.add(iter);
        // Size limitation to exactly numberOfNeighbors
        if(neighbors.size() >= numberOfNeighbors) {
          break;
        }
      }
      neighbors.sort();
      storage.put(iditer, neighbors);
      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);
  }

  @Override
  public ArrayDBIDs getNearestNeighborSet(DBIDRef objid) {
    if(storage == null) {
      initialize();
    }
    return storage.get(objid);
  }

  /**
   * Get the number of neighbors
   * 
   * @return NN size
   */
  @Override
  public int getNumberOfNeighbors() {
    return numberOfNeighbors;
  }

  /**
   * Factory class
   * 
   * @author Erich Schubert
   * 
   * @stereotype factory
   * @navassoc - create - SharedNearestNeighborPreprocessor
   */
  public static class Factory<O> implements SharedNearestNeighborIndex.Factory<O> {
    /**
     * Parameter to indicate the number of neighbors to be taken into account
     * for the shared-nearest-neighbor similarity.
     */
    public static final OptionID NUMBER_OF_NEIGHBORS_ID = new OptionID("sharedNearestNeighbors", "number of nearest neighbors to consider (at least 1)");

    /**
     * Parameter to indicate the distance function to be used to ascertain the
     * nearest neighbors.
     */
    public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("SNNDistance", "the distance function to asses the nearest neighbors");

    /**
     * Holds the number of nearest neighbors to be used.
     */
    protected int numberOfNeighbors;

    /**
     * Hold the distance function to be used.
     */
    protected Distance<O> distanceFunction;

    /**
     * Constructor.
     * 
     * @param numberOfNeighbors Number of neighbors
     * @param distance Distance function
     */
    public Factory(int numberOfNeighbors, Distance<O> distance) {
      super();
      this.numberOfNeighbors = numberOfNeighbors;
      this.distanceFunction = distance;
    }

    @Override
    public SharedNearestNeighborPreprocessor<O> instantiate(Relation<O> relation) {
      return new SharedNearestNeighborPreprocessor<>(relation, numberOfNeighbors, distanceFunction);
    }

    /**
     * Get the number of neighbors
     * 
     * @return NN size
     */
    @Override
    public int getNumberOfNeighbors() {
      return numberOfNeighbors;
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return distanceFunction.getInputTypeRestriction();
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     */
    public static class Par<O> implements Parameterizer {
      /**
       * Holds the number of nearest neighbors to be used.
       */
      protected int numberOfNeighbors;

      /**
       * Hold the distance function to be used.
       */
      protected Distance<O> distanceFunction;

      @Override
      public void configure(Parameterization config) {
        new IntParameter(NUMBER_OF_NEIGHBORS_ID) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .grab(config, x -> numberOfNeighbors = x);
        new ObjectParameter<Distance<O>>(DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
            .grab(config, x -> distanceFunction = x);
      }

      @Override
      public Factory<O> make() {
        return new Factory<>(numberOfNeighbors, distanceFunction);
      }
    }
  }
}
