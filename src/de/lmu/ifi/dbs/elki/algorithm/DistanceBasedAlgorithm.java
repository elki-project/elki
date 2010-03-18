package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Provides an abstract algorithm already setting the distance function.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 * @param <R> the type of result to retrieve from this Algorithm
 */
public abstract class DistanceBasedAlgorithm<O extends DatabaseObject, D extends Distance<D>, R extends Result> extends AbstractAlgorithm<O, R> {
  /**
   * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("algorithm.distancefunction", "Distance function to determine the distance between database objects.");

  /**
   * Parameter to specify the distance function to determine the distance
   * between database objects, must extend
   * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction}.
   * <p>
   * Key: {@code -algorithm.distancefunction}
   * </p>
   * <p>
   * Default value:
   * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction}
   * </p>
   */
  protected final ObjectParameter<DistanceFunction<O, D>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<O, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);

  /**
   * Holds the instance of the distance function specified by
   * {@link #DISTANCE_FUNCTION_PARAM}.
   */
  private DistanceFunction<O, D> distanceFunction;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  protected DistanceBasedAlgorithm(Parameterization config) {
    super(config);
    // parameter distance function
    if (config.grab(DISTANCE_FUNCTION_PARAM)) {
      distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }
  }

  /**
   * Returns the distanceFunction.
   * 
   * @return the distanceFunction
   */
  public DistanceFunction<O, D> getDistanceFunction() {
    return distanceFunction;
  }
}
