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
package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Very broad interface for distance based algorithms.
 * 
 * @author Erich Schubert
 * @since 0.1
 * 
 * @param <O> Object type
 */
public interface DistanceBasedAlgorithm<O> extends Algorithm {
  /**
   * OptionID for {@link #DISTANCE_FUNCTION_ID}.
   */
  OptionID DISTANCE_FUNCTION_ID = new OptionID("algorithm.distancefunction", "Distance function to determine the distance between database objects.");

  /**
   * Returns the distanceFunction.
   * 
   * @return the distanceFunction
   */
  DistanceFunction<? super O> getDistanceFunction();
}
