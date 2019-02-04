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
package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Adapter to use a primitive number-distance as similarity measure, by
 * computing 1/distance.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <O> Object type
 */
public class InvertedDistanceSimilarityFunction<O> implements PrimitiveSimilarityFunction<O> {
  /**
   * Parameter to specify the similarity function to derive the distance between
   * database objects from. Must extend
   * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction} .
   */
  public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("adapter.distancefunction", "Distance function to derive the similarity between database objects from.");

  /**
   * Holds the similarity function.
   */
  protected PrimitiveDistanceFunction<? super O> distanceFunction;

  @Override
  public SimpleTypeInformation<? super O> getInputTypeRestriction() {
    return distanceFunction.getInputTypeRestriction();
  }

  @Override
  public double similarity(O o1, O o2) {
    double dist = distanceFunction.distance(o1, o2);
    return dist > 0. ? 1. / dist : Double.POSITIVE_INFINITY;
  }
}