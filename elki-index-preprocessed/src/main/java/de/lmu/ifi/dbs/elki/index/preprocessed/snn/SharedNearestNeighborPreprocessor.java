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
package de.lmu.ifi.dbs.elki.index.preprocessed.snn;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.preprocessed.AbstractPreprocessorIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
 * @has - - - DistanceFunction
 * 
 * @param <O> the type of database objects the preprocessor can be applied to
 */
@Title("Shared Nearest Neighbor Preprocessor")
@Description("Computes the k nearest neighbors of objects of a certain database.")
public class SharedNearestNeighborPreprocessor<O> extends AbstractPreprocessorIndex<O, ArrayDBIDs> implements SharedNearestNeighborIndex<O> {
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
  protected DistanceFunction<O> distanceFunction;

  /**
   * Constructor.
   * 
   * @param relation Database to use
   * @param numberOfNeighbors Number of neighbors
   * @param distanceFunction Distance function
   */
  public SharedNearestNeighborPreprocessor(Relation<O> relation, int numberOfNeighbors, DistanceFunction<O> distanceFunction) {
    super(relation);
    this.numberOfNeighbors = numberOfNeighbors;
    this.distanceFunction = distanceFunction;
  }

  @Override
  public void initialize() {
    if(getLogger().isVerbose()) {
      getLogger().verbose("Assigning nearest neighbor lists to database objects");
    }
    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, ArrayDBIDs.class);
    KNNQuery<O> knnquery = QueryUtil.getKNNQuery(relation, distanceFunction, numberOfNeighbors);

    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("assigning nearest neighbor lists", relation.size(), getLogger()) : null;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      ArrayModifiableDBIDs neighbors = DBIDUtil.newArray(numberOfNeighbors);
      DBIDs kNN = knnquery.getKNNForDBID(iditer, numberOfNeighbors);
      for(DBIDIter iter = kNN.iter(); iter.valid(); iter.advance()) {
        // if(!id.equals(nid)) {
        neighbors.add(iter);
        // }
        // Size limitation to exactly numberOfNeighbors
        if(neighbors.size() >= numberOfNeighbors) {
          break;
        }
      }
      neighbors.sort();
      storage.put(iditer, neighbors);
      getLogger().incrementProcessed(progress);
    }
    getLogger().ensureCompleted(progress);
  }

  @Override
  public ArrayDBIDs getNearestNeighborSet(DBIDRef objid) {
    if(storage == null) {
      initialize();
    }
    return storage.get(objid);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public String getLongName() {
    return "SNN id index";
  }

  @Override
  public String getShortName() {
    return "SNN-index";
  }

  @Override
  public void logStatistics() {
    // No statistics to log.
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
    public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("SNNDistanceFunction", "the distance function to asses the nearest neighbors");

    /**
     * Holds the number of nearest neighbors to be used.
     */
    protected int numberOfNeighbors;

    /**
     * Hold the distance function to be used.
     */
    protected DistanceFunction<O> distanceFunction;

    /**
     * Constructor.
     * 
     * @param numberOfNeighbors Number of neighbors
     * @param distanceFunction Distance function
     */
    public Factory(int numberOfNeighbors, DistanceFunction<O> distanceFunction) {
      super();
      this.numberOfNeighbors = numberOfNeighbors;
      this.distanceFunction = distanceFunction;
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
    public static class Parameterizer<O> extends AbstractParameterizer {
      /**
       * Holds the number of nearest neighbors to be used.
       */
      protected int numberOfNeighbors;

      /**
       * Hold the distance function to be used.
       */
      protected DistanceFunction<O> distanceFunction;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        final IntParameter numberOfNeighborsP = new IntParameter(NUMBER_OF_NEIGHBORS_ID) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(numberOfNeighborsP)) {
          numberOfNeighbors = numberOfNeighborsP.getValue();
        }

        final ObjectParameter<DistanceFunction<O>> distanceFunctionP = new ObjectParameter<>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
        if(config.grab(distanceFunctionP)) {
          distanceFunction = distanceFunctionP.instantiateClass(config);
        }
      }

      @Override
      protected Factory<O> makeInstance() {
        return new Factory<>(numberOfNeighbors, distanceFunction);
      }
    }
  }
}
