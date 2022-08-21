/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.application;

import elki.Algorithm;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.workflow.InputStep;

/**
 * Abstract base class for distance-based tasks and experiments.
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @param <O> Object type
 */
public abstract class AbstractDistanceBasedApplication<O> extends AbstractApplication {
  /**
   * Distance function to use.
   */
  protected Distance<? super O> distance;

  /**
   * Data input step
   */
  protected InputStep inputstep;

  /**
   * Constructor.
   */
  public AbstractDistanceBasedApplication(InputStep inputstep, Distance<? super O> distance) {
    super();
    this.inputstep = inputstep;
    this.distance = distance;
  }

  /**
   * Parameterization class
   *
   * @hidden
   *
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
  public abstract static class Par<O> extends AbstractApplication.Par {
    /**
     * Data input step
     */
    protected InputStep inputstep;

    /**
     * Distance function to use
     */
    protected Distance<? super O> distance;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      // Data input
      inputstep = config.tryInstantiate(InputStep.class);
      // Distance function
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
    }
  }
}
