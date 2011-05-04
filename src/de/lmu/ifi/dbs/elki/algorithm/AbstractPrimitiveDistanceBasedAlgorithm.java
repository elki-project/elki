package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Provides an abstract algorithm already setting the distance function.
 * 
 * This class only allows distances that are defined on arbitrary objects, not only database objects!
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.has PrimitiveDistanceFunction
 * @apiviz.excludeSubtypes
 * 
 * @param <O> the type of objects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 * @param <R> the type of result to retrieve from this Algorithm
 */
public abstract class AbstractPrimitiveDistanceBasedAlgorithm<O, D extends Distance<D>, R extends Result> extends AbstractAlgorithm<R> {
  /**
   * Holds the instance of the distance function specified by
   * {@link #DISTANCE_FUNCTION_PARAM}.
   */
  private PrimitiveDistanceFunction<? super O, D> distanceFunction;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   */
  protected AbstractPrimitiveDistanceBasedAlgorithm(PrimitiveDistanceFunction<? super O, D> distanceFunction) {
    super();
    this.distanceFunction = distanceFunction;
  }

  /**
   * Returns the distanceFunction.
   * 
   * @return the distanceFunction
   */
  public PrimitiveDistanceFunction<? super O, D> getDistanceFunction() {
    return distanceFunction;
  }

  /**
   * Parameterization helper class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public abstract static class Parameterizer<O, D extends Distance<D>> extends AbstractParameterizer {
    protected PrimitiveDistanceFunction<O, D> distanceFunction;
    
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<PrimitiveDistanceFunction<O, D>> distanceFunctionP = makeParameterDistanceFunction(EuclideanDistanceFunction.class, PrimitiveDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }
    }
  }
}