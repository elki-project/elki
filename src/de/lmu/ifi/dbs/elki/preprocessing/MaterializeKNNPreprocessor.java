package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.progress.FiniteProgress;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 * 
 * Used for example by {@link de.lmu.ifi.dbs.elki.algorithm.outlier.LOF}.
 * 
 * @author Erich Schubert
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
public class MaterializeKNNPreprocessor<O extends DatabaseObject, D extends Distance<D>> extends AbstractParameterizable implements Preprocessor<O> {
  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("materialize.k", "The number of nearest neighbors of an object to be materialized.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * materialized. must be an integer greater than 1.
   * <p>
   * Key: {@code -materialize.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(1));

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  int k;

  /**
   * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("materialize.distance", "the distance function to materialize the nearest neighbors");

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
  public final ClassParameter<DistanceFunction<O, D>> DISTANCE_FUNCTION_PARAM = new ClassParameter<DistanceFunction<O, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class.getName());

  /**
   * Hold the distance function to be used.
   */
  private DistanceFunction<O, D> distanceFunction;
  
  /**
   * Materialized neighborhood
   */
  private HashMap<Integer, List<DistanceResultPair<D>>> materialized;

  /**
   * Provides a k nearest neighbors Preprocessor.
   */
  public MaterializeKNNPreprocessor() {
    super();
    addOption(K_PARAM);
    addOption(DISTANCE_FUNCTION_PARAM);
  }

  /**
   * Annotates the nearest neighbors based on the values of
   * {@link #k} and {@link #distanceFunction} to each database
   * object.
   */
  public void run(Database<O> database, boolean verbose, boolean time) {
    distanceFunction.setDatabase(database, verbose, time);
    materialized = new HashMap<Integer, List<DistanceResultPair<D>>>(database.size());
    if(logger.isVerbose()) {
      logger.verbose("Assigning nearest neighbor lists to database objects");
    }
    FiniteProgress preprocessing = new FiniteProgress("Materializing k nearest neighbors (k="+k+")", database.size());
    int count = 0;
    for(Integer id : database) {
      List<DistanceResultPair<D>> kNN = database.kNNQueryForID(id, k, distanceFunction);
      materialized.put(id, kNN);
      if(logger.isVerbose()) {
        count++;
        preprocessing.setProcessed(count);
        logger.progress(preprocessing);
      }
    }
    if(logger.isVerbose()) {
      logger.verbose("kNN materialization completed.");
    }
  }

  /**
   * Sets the parameter values of {@link #K_PARAM} and
   * {@link #DISTANCE_FUNCTION_PARAM} to {@link #k} and
   * {@link #distanceFunction}, respectively.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // number of neighbors
    k = K_PARAM.getValue();

    // distance function
    distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass();
    remainingParameters = distanceFunction.setParameters(remainingParameters);
    addParameterizable(distanceFunction);
    
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Provides a short description of the purpose of this class.
   */
  @Override
  public String parameterDescription() {
    StringBuffer description = new StringBuffer();
    description.append(MaterializeKNNPreprocessor.class.getName());
    description.append(" materializes the k nearest neighbors of objects of a database.\n");
    description.append(super.parameterDescription());
    return description.toString();
  }

  /**
   * Materialize a neighborhood.
   * 
   * @return the materialized neighborhoods
   */
  public HashMap<Integer, List<DistanceResultPair<D>>> getMaterialized() {
    return materialized;
  }
}