package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
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
 * This functionality is similar but not identical to {@link MaterializeKNNPreprocessor}:
 * While it also computes the k nearest neighbors, it does not keep the actual distances,
 * but organizes the NN set in a TreeSet for fast set operations.
 * 
 * @author Arthur Zimek
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
@Title("Shared nearest neighbor Preprocessor")
@Description("Computes the k nearest neighbors of objects of a certain database.")
public class SharedNearestNeighborsPreprocessor<O extends DatabaseObject, D extends Distance<D>> extends AbstractLoggable implements Preprocessor<O, SortedSet<Integer>>, Parameterizable {
  /**
   * OptionID for {@link #NUMBER_OF_NEIGHBORS_PARAM}
   */
  public static final OptionID NUMBER_OF_NEIGHBORS_ID = OptionID.getOrCreateOptionID("sharedNearestNeighbors", "number of nearest neighbors to consider (at least 1)");

  /**
   * Parameter to indicate the number of neighbors to be taken into account for
   * the shared-nearest-neighbor similarity.
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
  private int numberOfNeighbors;

  /**
   * Hold the distance function to be used.
   */
  private DistanceFunction<O, D> distanceFunction;

  /**
   * Data storage
   */
  private HashMap<Integer, SortedSet<Integer>> sharedNearestNeighbors = new HashMap<Integer, SortedSet<Integer>>();

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public SharedNearestNeighborsPreprocessor(Parameterization config) {
    super();
    // number of neighbors
    if(config.grab(NUMBER_OF_NEIGHBORS_PARAM)) {
      numberOfNeighbors = NUMBER_OF_NEIGHBORS_PARAM.getValue();
    }

    // distance function
    if(config.grab(DISTANCE_FUNCTION_PARAM)) {
      distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }
  }

  /**
   * Annotates the nearest neighbors based on the values of
   * {@link #numberOfNeighbors} and {@link #distanceFunction} to each database
   * object.
   */
  public void run(Database<O> database, @SuppressWarnings("unused") boolean verbose, @SuppressWarnings("unused") boolean time) {
    distanceFunction.setDatabase(database);
    if(logger.isVerbose()) {
      logger.verbose("Assigning nearest neighbor lists to database objects");
    }
    FiniteProgress preprocessing = new FiniteProgress("assigning nearest neighbor lists", database.size());
    int count = 0;
    for(Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
      count++;
      Integer id = iter.next();
      List<Integer> neighbors = new ArrayList<Integer>(numberOfNeighbors);
      List<DistanceResultPair<D>> kNN = database.kNNQueryForID(id, numberOfNeighbors, distanceFunction);
      for(int i = 1; i < kNN.size(); i++) {
        neighbors.add(kNN.get(i).getID());
      }
      SortedSet<Integer> set = new TreeSet<Integer>(neighbors);
      sharedNearestNeighbors.put(id, set);
      if(logger.isVerbose()) {
        preprocessing.setProcessed(count);
        logger.progress(preprocessing);
      }
    }
  }

  /**
   * Returns the number of nearest neighbors considered
   * 
   * @return number of neighbors considered
   */
  public int getNumberOfNeighbors() {
    return numberOfNeighbors;
  }

  /**
   * Returns the distance function used by the preprocessor.
   * 
   * @return distance function used.
   */
  public DistanceFunction<O, D> getDistanceFunction() {
    return distanceFunction;
  }

  @Override
  public SortedSet<Integer> get(Integer id) {
    return sharedNearestNeighbors.get(id);
  }
}