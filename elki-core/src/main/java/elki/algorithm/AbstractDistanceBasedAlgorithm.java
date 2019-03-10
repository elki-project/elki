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

import elki.AbstractAlgorithm;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for distance-based algorithms.
 *
 * @author Arthur Zimek
 * @since 0.1
 *
 * @opt nodefillcolor LemonChiffon
 * @has - - - Distance
 *
 * @param <O> the type of objects handled by this algorithm
 * @param <R> the type of result to retrieve from this Algorithm
 */
public abstract class AbstractDistanceBasedAlgorithm<O, R> extends AbstractAlgorithm<R> {
  /**
   * Holds the instance of the distance function specified by
   * {@link Parameterizer#DISTANCE_FUNCTION_ID}.
   */
  private Distance<? super O> distanceFunction;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   */
  protected AbstractDistanceBasedAlgorithm(Distance<? super O> distanceFunction) {
    super();
    this.distanceFunction = distanceFunction;
  }

  /**
   * Returns the distanceFunction.
   *
   * @return the distanceFunction
   */
  public Distance<? super O> getDistance() {
    return distanceFunction;
  }

  /**
   * Parameterization helper class.
   *
   * @author Erich Schubert
   */
  public abstract static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * OptionID for the distance function.
     */
    public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("algorithm.distancefunction", "Distance function to determine the distance between database objects.");

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distanceFunction;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<Distance<? super O>> distanceFunctionP = new ObjectParameter<>(DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }
    }
  }
}
