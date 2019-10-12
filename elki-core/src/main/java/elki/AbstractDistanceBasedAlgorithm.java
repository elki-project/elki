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
package elki;

import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.utilities.optionhandling.Parameterizer;
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
 * @param <D> the type of distance function used by this algorithm
 * @param <R> the type of result to retrieve from this Algorithm
 */
public abstract class AbstractDistanceBasedAlgorithm<D extends Distance<?>, R> extends AbstractAlgorithm<R> {
  /**
   * Holds the instance of the distance function specified by
   * {@link Par#DISTANCE_FUNCTION_ID}.
   */
  protected D distance;

  /**
   * Constructor.
   *
   * @param distance Distance function
   */
  protected AbstractDistanceBasedAlgorithm(D distance) {
    super();
    this.distance = distance;
  }

  /**
   * Returns the distance.
   *
   * @return the distance
   */
  public D getDistance() {
    return distance;
  }

  /**
   * Parameterization helper class.
   *
   * @author Erich Schubert
   */
  public abstract static class Par<D extends Distance<?>> implements Parameterizer {
    /**
     * OptionID for the distance function.
     */
    public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("algorithm.distancefunction", "Distance function to determine the distance between database objects.");

    /**
     * The distance function to use.
     */
    protected D distance;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<D>(DISTANCE_FUNCTION_ID, getDistanceRestriction(), getDefaultDistance()) //
          .grab(config, x -> distance = x);
    }

    /**
     * Get the minimum requirements for the distance function.
     *
     * @return Restriction class
     */
    public Class<?> getDistanceRestriction() {
      return Distance.class;
    }

    /**
     * The default distance function.
     *
     * @return Default distance function
     */
    public Class<?> getDefaultDistance() {
      return EuclideanDistance.class;
    }
  }
}
