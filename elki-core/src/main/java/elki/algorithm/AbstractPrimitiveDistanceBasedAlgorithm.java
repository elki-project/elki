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
package elki.algorithm;

import elki.distance.distancefunction.PrimitiveDistance;
import elki.distance.distancefunction.minkowski.EuclideanDistance;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for distance-based algorithms that need to work with
 * synthetic objects such as mean vectors.
 * 
 * This class only allows distances that are defined on arbitrary objects, not
 * only database objects!
 * 
 * @author Arthur Zimek
 * @since 0.1
 * 
 * @has - - - PrimitiveDistance
 * 
 * @param <O> the type of objects handled by this algorithm
 * @param <R> the type of result to retrieve from this Algorithm
 */
public abstract class AbstractPrimitiveDistanceBasedAlgorithm<O, R> extends AbstractAlgorithm<R> {
  /**
   * Holds the instance of the distance function specified by
   * {@link DistanceBasedAlgorithm#DISTANCE_FUNCTION_ID}.
   */
  protected PrimitiveDistance<? super O> distanceFunction;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   */
  protected AbstractPrimitiveDistanceBasedAlgorithm(PrimitiveDistance<? super O> distanceFunction) {
    super();
    this.distanceFunction = distanceFunction;
  }

  /**
   * Returns the distanceFunction.
   * 
   * @return the distanceFunction
   */
  public PrimitiveDistance<? super O> getDistance() {
    return distanceFunction;
  }

  /**
   * Parameterization helper class.
   * 
   * @author Erich Schubert
   */
  public abstract static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Distance function to use.
     */
    protected PrimitiveDistance<O> distanceFunction;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<PrimitiveDistance<O>> distanceFunctionP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, PrimitiveDistance.class, EuclideanDistance.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }
    }
  }
}