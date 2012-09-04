package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.reinsert;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for reinsertion strategies that have a "relative amount"
 * parameter to partially reinsert entries.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractPartialReinsert implements ReinsertStrategy {
  /**
   * Amount of entries to reinsert
   */
  protected double reinsertAmount = 0.3;

  /**
   * Distance function to use for measuring
   */
  SpatialPrimitiveDoubleDistanceFunction<?> distanceFunction;

  /**
   * Constructor.
   * 
   * @param reinsertAmount Relative amount of objects to reinsert.
   * @param distanceFunction Distance function to use
   */
  public AbstractPartialReinsert(double reinsertAmount, SpatialPrimitiveDoubleDistanceFunction<?> distanceFunction) {
    super();
    this.reinsertAmount = reinsertAmount;
    this.distanceFunction = distanceFunction;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Reinsertion share
     */
    public static OptionID REINSERT_AMOUNT_ID = OptionID.getOrCreateOptionID("rtree.reinsertion-amount", "The amount of entries to reinsert.");

    /**
     * Reinsertion share
     */
    public static OptionID REINSERT_DISTANCE_ID = OptionID.getOrCreateOptionID("rtree.reinsertion-distancce", "The distance function to compute reinsertion candidates by.");

    /**
     * The actual reinsertion strategy
     */
    double reinsertAmount = 0.3;

    /**
     * Distance function to use for measuring
     */
    SpatialPrimitiveDoubleDistanceFunction<?> distanceFunction;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter reinsertAmountP = new DoubleParameter(REINSERT_AMOUNT_ID, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.OPEN, 0.5, IntervalConstraint.IntervalBoundary.OPEN), 0.3);
      if(config.grab(reinsertAmountP)) {
        reinsertAmount = reinsertAmountP.getValue();
      }
      ObjectParameter<SpatialPrimitiveDoubleDistanceFunction<?>> distanceP = new ObjectParameter<SpatialPrimitiveDoubleDistanceFunction<?>>(REINSERT_DISTANCE_ID, SpatialPrimitiveDoubleDistanceFunction.class, SquaredEuclideanDistanceFunction.class);
      if(config.grab(distanceP)) {
        distanceFunction = distanceP.instantiateClass(config);
      }
    }
  }
}
