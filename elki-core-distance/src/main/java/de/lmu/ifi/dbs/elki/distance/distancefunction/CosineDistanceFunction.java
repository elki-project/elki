/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Cosine distance function for feature vectors.
 * 
 * The cosine distance is computed from the cosine similarity by
 * <code>1-(cosine similarity)</code>.
 * 
 * Cosine similarity is defined as
 * \[ \frac{\vec{x}\cdot\vec{y}}{||a||\cdot||b||} \]
 * Cosine distance then is defined as
 * \[ 1 - \frac{\vec{x}\cdot\vec{y}}{||a||\cdot||b||} \in [0;2] \]
 * 
 * {@link ArcCosineDistanceFunction} may sometimes be more appropriate, but also
 * more computationally expensive.
 * 
 * @author Arthur Zimek
 * @since 0.2
 */
@Priority(Priority.IMPORTANT)
@Alias({ "cosine" })
public class CosineDistanceFunction implements SpatialPrimitiveDistanceFunction<NumberVector>, NumberVectorDistanceFunction<NumberVector> {
  /**
   * Static instance
   */
  public static final CosineDistanceFunction STATIC = new CosineDistanceFunction();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance
   */
  @Deprecated
  public CosineDistanceFunction() {
    super();
  }

  /**
   * Computes the cosine distance for two given feature vectors.
   * 
   * The cosine distance is computed from the cosine similarity by
   * <code>1-(cosine similarity)</code>.
   * 
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the cosine distance for two given feature vectors v1 and v2
   */
  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    double d = VectorUtil.cosAngle(v1, v2);
    return (d <= 1) ? 1 - d : 0;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    double d = VectorUtil.minCosAngle(mbr1, mbr2);
    return (d <= 1) ? 1 - d : 0;
  }

  @Override
  public String toString() {
    return "CosineDistance";
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof CosineDistanceFunction;
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return NumberVector.VARIABLE_LENGTH;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected CosineDistanceFunction makeInstance() {
      return CosineDistanceFunction.STATIC;
    }
  }
}
