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

import elki.distance.distancefunction.NumberVectorDistance;
import elki.distance.distancefunction.minkowski.EuclideanDistance;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for distance-based algorithms that need to work with
 * synthetic numerical vectors such as <b>mean</b> vectors.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - NumberVectorDistance
 *
 * @param <O> Object type
 * @param <R> the type of result to retrieve from this Algorithm
 */
public abstract class AbstractNumberVectorDistanceBasedAlgorithm<O, R> extends AbstractAlgorithm<R>implements DistanceBasedAlgorithm<O> {
  /**
   * Holds the instance of the distance function specified by
   * {@link DistanceBasedAlgorithm#DISTANCE_FUNCTION_ID}.
   */
  protected NumberVectorDistance<? super O> distanceFunction;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   */
  protected AbstractNumberVectorDistanceBasedAlgorithm(NumberVectorDistance<? super O> distanceFunction) {
    super();
    this.distanceFunction = distanceFunction;
  }

  /**
   * Returns the distanceFunction.
   *
   * @return the distanceFunction
   */
  @Override
  final public NumberVectorDistance<? super O> getDistance() {
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
    protected NumberVectorDistance<? super O> distanceFunction;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<NumberVectorDistance<? super O>> distanceFunctionP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, NumberVectorDistance.class, EuclideanDistance.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }
    }
  }
}