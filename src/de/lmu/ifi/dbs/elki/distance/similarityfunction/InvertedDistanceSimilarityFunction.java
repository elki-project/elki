package de.lmu.ifi.dbs.elki.distance.similarityfunction;

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
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Adapter to use a primitive number-distance as similarity measure, by computing
 * 1/distance.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class InvertedDistanceSimilarityFunction<O> extends AbstractPrimitiveSimilarityFunction<O, DoubleDistance> {
  /**
   * Parameter to specify the similarity function to derive the distance between
   * database objects from. Must extend
   * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction} .
   * <p>
   * Key: {@code -adapter.distancefunction}
   * </p>
   * <p>
   * Default value:
   * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction}
   * </p>
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("adapter.distancefunction", "Distance function to derive the similarity between database objects from.");

  /**
   * Holds the similarity function.
   */
  protected PrimitiveDistanceFunction<? super O, ? extends NumberDistance<?, ?>> distanceFunction;

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  public SimpleTypeInformation<? super O> getInputTypeRestriction() {
    return distanceFunction.getInputTypeRestriction();
  }

  @Override
  public DoubleDistance similarity(O o1, O o2) {
    double dist = distanceFunction.distance(o1, o2).doubleValue();
    return new DoubleDistance(1. / dist);
  }
}