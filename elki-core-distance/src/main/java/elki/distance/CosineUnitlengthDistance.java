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
package elki.distance;

import elki.data.NumberVector;
import elki.data.VectorUtil;
import elki.data.spatial.SpatialComparable;
import elki.data.type.SimpleTypeInformation;
import elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Cosine distance function for <em>unit length</em> feature vectors.
 * <p>
 * The cosine distance is computed from the cosine similarity by
 * <code>1-(cosine similarity)</code>.
 * <p>
 * Cosine similarity is defined as
 * \[ \tfrac{\vec{x}\cdot\vec{y}}{||a||\cdot||b||}
 * =_{||a||=||b||=1} \vec{x}\cdot\vec{y}
 * \]
 * Cosine distance then is defined as
 * \[ 1 - \tfrac{\vec{x}\cdot\vec{y}}{||a||\cdot||b||}
 * =_{||a||=||b||=1} 1-\vec{x}\cdot\vec{y} \in [0;2] \]
 * <p>
 * This implementation <em>assumes</em> that \(||a||=||b||=1\). If this does not
 * hold for your data, use {@link CosineDistance} instead!
 * <p>
 * {@link ArcCosineUnitlengthDistance} may sometimes be more
 * appropriate, but also more computationally expensive.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class CosineUnitlengthDistance implements SpatialPrimitiveDistance<NumberVector>, NumberVectorDistance<NumberVector> {
  /**
   * Static instance
   */
  public static final CosineUnitlengthDistance STATIC = new CosineUnitlengthDistance();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance
   */
  @Deprecated
  public CosineUnitlengthDistance() {
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
    double d = VectorUtil.dot(v1, v2);
    return (d <= 1) ? 1 - d : 0;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    double d = VectorUtil.minDot(mbr1, mbr2);
    return (d <= 1) ? 1 - d : 0;
  }

  @Override
  public String toString() {
    return "CosineUnitlengthDistance";
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
    protected CosineUnitlengthDistance makeInstance() {
      return CosineUnitlengthDistance.STATIC;
    }
  }
}
