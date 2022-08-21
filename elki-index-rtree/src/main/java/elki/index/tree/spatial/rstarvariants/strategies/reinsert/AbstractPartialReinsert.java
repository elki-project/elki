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
package elki.index.tree.spatial.rstarvariants.strategies.reinsert;

import elki.distance.SpatialPrimitiveDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for reinsertion strategies that have a "relative amount"
 * parameter to partially reinsert entries.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public abstract class AbstractPartialReinsert implements ReinsertStrategy {
  /**
   * Amount of entries to reinsert
   */
  protected double reinsertAmount = 0.3;

  /**
   * Distance function to use for measuring
   */
  SpatialPrimitiveDistance<?> distance;

  /**
   * Constructor.
   * 
   * @param reinsertAmount Relative amount of objects to reinsert.
   * @param distance Distance function to use
   */
  public AbstractPartialReinsert(double reinsertAmount, SpatialPrimitiveDistance<?> distance) {
    super();
    this.reinsertAmount = reinsertAmount;
    this.distance = distance;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public abstract static class Par implements Parameterizer {
    /**
     * Reinsertion share
     */
    public static final OptionID REINSERT_AMOUNT_ID = new OptionID("rtree.reinsertion-amount", "The amount of entries to reinsert.");

    /**
     * Reinsertion share
     */
    public static final OptionID REINSERT_DISTANCE_ID = new OptionID("rtree.reinsertion-distancce", "The distance function to compute reinsertion candidates by.");

    /**
     * The actual reinsertion strategy
     */
    double reinsertAmount = 0.3;

    /**
     * Distance function to use for measuring
     */
    SpatialPrimitiveDistance<?> distance;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(REINSERT_AMOUNT_ID, 0.3) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_HALF_DOUBLE) //
          .grab(config, x -> reinsertAmount = x);
      new ObjectParameter<SpatialPrimitiveDistance<?>>(REINSERT_DISTANCE_ID, SpatialPrimitiveDistance.class, SquaredEuclideanDistance.class) //
          .grab(config, x -> distance = x);
    }
  }
}
