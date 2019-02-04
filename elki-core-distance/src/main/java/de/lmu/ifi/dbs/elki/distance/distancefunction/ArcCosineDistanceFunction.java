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
package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Arcus cosine distance function for feature vectors.
 * <p>
 * The arc cosine distance is computed as the arcus from the cosine similarity
 * value, i.e., <code>arccos(&lt;v1,v2&gt;)</code>.
 * <p>
 * Cosine similarity is defined as
 * \[ \tfrac{\vec{x}\cdot\vec{y}}{||a||\cdot||b||} \]
 * Arcus cosine distance then is
 * \[ \text{arccos} \tfrac{\vec{x}\cdot\vec{y}}{||a||\cdot||b||} \in [0;\pi] \]
 * <p>
 * {@link CosineDistanceFunction} is a bit less expensive, and will yield the
 * same ranking of neighbors.
 *
 * @author Arthur Zimek
 * @since 0.2
 */
@Priority(Priority.IMPORTANT)
@Alias({ "arccos" })
public class ArcCosineDistanceFunction implements SpatialPrimitiveDistanceFunction<NumberVector>, NumberVectorDistanceFunction<NumberVector> {
  /**
   * Static instance
   */
  public static final ArcCosineDistanceFunction STATIC = new ArcCosineDistanceFunction();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public ArcCosineDistanceFunction() {
    super();
  }

  /**
   * Computes the cosine distance for two given feature vectors.
   * 
   * The cosine distance is computed as the arcus from the cosine similarity
   * value, i.e., <code>arccos(&lt;v1,v2&gt;)</code>.
   * 
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the cosine distance for two given feature vectors v1 and v2
   */
  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    double d = Math.acos(VectorUtil.cosAngle(v1, v2));
    return d > 0 ? d : 0; // Avoid NaN
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    double d = Math.acos(VectorUtil.minCosAngle(mbr1, mbr2));
    return d > 0 ? d : 0; // Avoid NaN
  }

  @Override
  public boolean isMetric() {
    return true; // on the non-negative unit sphere.
  }

  @Override
  public String toString() {
    return "ArcCosineDistance";
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj != null && this.getClass().equals(obj.getClass()));
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
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected ArcCosineDistanceFunction makeInstance() {
      return ArcCosineDistanceFunction.STATIC;
    }
  }
}
