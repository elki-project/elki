package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Provides an abstract algorithm already setting the distance function.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.landmark
 * @apiviz.has DistanceFunction
 * @apiviz.excludeSubtypes
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 * @param <R> the type of result to retrieve from this Algorithm
 */
public abstract class AbstractDistanceBasedAlgorithm<O extends DatabaseObject, D extends Distance<D>, R extends Result> extends AbstractAlgorithm<O, R> {
  /**
   * OptionID for {@link #DISTANCE_FUNCTION_ID}
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("algorithm.distancefunction", "Distance function to determine the distance between database objects.");

  /**
   * Holds the instance of the distance function specified by
   * {@link #DISTANCE_FUNCTION_ID}.
   */
  private DistanceFunction<? super O, D> distanceFunction;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  protected AbstractDistanceBasedAlgorithm(Parameterization config) {
    super();
    config = config.descend(this);
    distanceFunction = getParameterDistanceFunction(config, EuclideanDistanceFunction.class, DistanceFunction.class);
  }

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   */
  protected AbstractDistanceBasedAlgorithm(DistanceFunction<? super O, D> distanceFunction) {
    super();
    this.distanceFunction = distanceFunction;
  }

  /**
   * Returns the distanceFunction.
   * 
   * @return the distanceFunction
   */
  public DistanceFunction<? super O, D> getDistanceFunction() {
    return distanceFunction;
  }
}