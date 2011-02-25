package de.lmu.ifi.dbs.elki.index.preprocessed.snn;

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.preprocessed.AbstractPreprocessorIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * A preprocessor for annotation of the ids of nearest neighbors to each
 * database object.
 * <p/>
 * The k nearest neighbors are assigned based on an arbitrary distance function.
 * 
 * This functionality is similar but not identical to
 * {@link MaterializeKNNPreprocessor}: While it also computes the k nearest
 * neighbors, it does not keep the actual distances, but organizes the NN set in
 * a TreeSet for fast set operations.
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * 
 * @apiviz.has DistanceFunction
 * 
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
@Title("Shared nearest neighbor Preprocessor")
@Description("Computes the k nearest neighbors of objects of a certain database.")
public class SharedNearestNeighborPreprocessor<O extends DatabaseObject, D extends Distance<D>> extends AbstractPreprocessorIndex<O, TreeSetDBIDs> implements SharedNearestNeighborIndex<O> {
  /**
   * Get a logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SharedNearestNeighborPreprocessor.class);

  /**
   * Database we are attached to
   */
  final protected Database<O> database;

  /**
   * Holds the number of nearest neighbors to be used.
   */
  protected int numberOfNeighbors;

  /**
   * Hold the distance function to be used.
   */
  protected DistanceFunction<O, D> distanceFunction;

  /**
   * Constructor.
   * 
   * @param database Database to use
   * @param numberOfNeighbors Number of neighbors
   * @param distanceFunction Distance function
   */
  public SharedNearestNeighborPreprocessor(Database<O> database, int numberOfNeighbors, DistanceFunction<O, D> distanceFunction) {
    super();
    this.database = database;
    this.numberOfNeighbors = numberOfNeighbors;
    this.distanceFunction = distanceFunction;
  }

  /**
   * Preprocessing step.
   */
  protected void preprocess() {
    if(getLogger().isVerbose()) {
      getLogger().verbose("Assigning nearest neighbor lists to database objects");
    }
    storage = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, SetDBIDs.class);
    KNNQuery<O, D> knnquery = database.getKNNQuery(distanceFunction, numberOfNeighbors + 1);

    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("assigning nearest neighbor lists", database.size(), getLogger()) : null;
    int count = 0;
    for(Iterator<DBID> iter = database.iterator(); iter.hasNext();) {
      count++;
      DBID id = iter.next();
      TreeSetModifiableDBIDs neighbors = DBIDUtil.newTreeSet(numberOfNeighbors);
      List<DistanceResultPair<D>> kNN = knnquery.getKNNForDBID(id, numberOfNeighbors + 1);
      for(int i = 0; i < kNN.size(); i++) {
        final DBID nid = kNN.get(i).getID();
        if(!id.equals(nid)) {
          neighbors.add(nid);
        }
        // Size limitation to exaclty numberOfNeighbors
        if (neighbors.size() >= numberOfNeighbors) {
          break;
        }
      }
      storage.put(id, neighbors);
      if(progress != null) {
        progress.incrementProcessed(getLogger());
      }
    }
    if(progress != null) {
      progress.ensureCompleted(getLogger());
    }
  }

  @Override
  public TreeSetDBIDs getNearestNeighborSet(DBID objid) {
    if(storage == null) {
      preprocess();
    }
    return storage.get(objid);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public String getLongName() {
    return "SNN id index";
  }

  @Override
  public String getShortName() {
    return "SNN-index";
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
   * @apiviz.stereotype factory
   * @apiviz.uses SharedNearestNeighborPreprocessor oneway - - «create»
   */
  public static class Factory<O extends DatabaseObject, D extends Distance<D>> implements SharedNearestNeighborIndex.Factory<O, SharedNearestNeighborPreprocessor<O, D>>, Parameterizable {
    /**
     * OptionID for {@link #NUMBER_OF_NEIGHBORS_PARAM}
     */
    public static final OptionID NUMBER_OF_NEIGHBORS_ID = OptionID.getOrCreateOptionID("sharedNearestNeighbors", "number of nearest neighbors to consider (at least 1)");

    /**
     * Parameter to indicate the number of neighbors to be taken into account
     * for the shared-nearest-neighbor similarity.
     * <p/>
     * <p>
     * Default value: 1
     * </p>
     * <p>
     * Key: {@code sharedNearestNeighbors}
     * </p>
     */
    private final IntParameter NUMBER_OF_NEIGHBORS_PARAM = new IntParameter(NUMBER_OF_NEIGHBORS_ID, new GreaterEqualConstraint(1), 1);

    /**
     * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
     */
    public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("SNNDistanceFunction", "the distance function to asses the nearest neighbors");

    /**
     * Parameter to indicate the distance function to be used to ascertain the
     * nearest neighbors.
     * <p/>
     * <p>
     * Default value: {@link EuclideanDistanceFunction}
     * </p>
     * <p>
     * Key: {@code SNNDistanceFunction}
     * </p>
     */
    private final ObjectParameter<DistanceFunction<O, D>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<O, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);

    /**
     * Holds the number of nearest neighbors to be used.
     */
    protected int numberOfNeighbors;

    /**
     * Hold the distance function to be used.
     */
    protected DistanceFunction<O, D> distanceFunction;

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     * 
     * @param config Parameterization
     */
    public Factory(Parameterization config) {
      super();
      config = config.descend(this);
      // number of neighbors
      if(config.grab(NUMBER_OF_NEIGHBORS_PARAM)) {
        numberOfNeighbors = NUMBER_OF_NEIGHBORS_PARAM.getValue();
      }

      // distance function
      if(config.grab(DISTANCE_FUNCTION_PARAM)) {
        distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
      }
    }

    @Override
    public SharedNearestNeighborPreprocessor<O, D> instantiate(Database<O> database) {
      return new SharedNearestNeighborPreprocessor<O, D>(database, numberOfNeighbors, distanceFunction);
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
  }
}