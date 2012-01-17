package de.lmu.ifi.dbs.elki.algorithm;

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
   * {@link AbstractDistanceBasedAlgorithm#DISTANCE_FUNCTION_ID}.
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