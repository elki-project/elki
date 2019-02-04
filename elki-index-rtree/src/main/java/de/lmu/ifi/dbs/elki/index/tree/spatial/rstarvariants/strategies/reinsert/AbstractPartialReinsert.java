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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.reinsert;

import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
  SpatialPrimitiveDistanceFunction<?> distanceFunction;

  /**
   * Constructor.
   * 
   * @param reinsertAmount Relative amount of objects to reinsert.
   * @param distanceFunction Distance function to use
   */
  public AbstractPartialReinsert(double reinsertAmount, SpatialPrimitiveDistanceFunction<?> distanceFunction) {
    super();
    this.reinsertAmount = reinsertAmount;
    this.distanceFunction = distanceFunction;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Reinsertion share
     */
    public static OptionID REINSERT_AMOUNT_ID = new OptionID("rtree.reinsertion-amount", "The amount of entries to reinsert.");

    /**
     * Reinsertion share
     */
    public static OptionID REINSERT_DISTANCE_ID = new OptionID("rtree.reinsertion-distancce", "The distance function to compute reinsertion candidates by.");

    /**
     * The actual reinsertion strategy
     */
    double reinsertAmount = 0.3;

    /**
     * Distance function to use for measuring
     */
    SpatialPrimitiveDistanceFunction<?> distanceFunction;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter reinsertAmountP = new DoubleParameter(REINSERT_AMOUNT_ID, 0.3) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_HALF_DOUBLE);
      if(config.grab(reinsertAmountP)) {
        reinsertAmount = reinsertAmountP.getValue();
      }
      ObjectParameter<SpatialPrimitiveDistanceFunction<?>> distanceP = new ObjectParameter<>(REINSERT_DISTANCE_ID, SpatialPrimitiveDistanceFunction.class, SquaredEuclideanDistanceFunction.class);
      if(config.grab(distanceP)) {
        distanceFunction = distanceP.instantiateClass(config);
      }
    }
  }
}
