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
import elki.utilities.optionhandling.Parameterizer;

/**
 * Arcus cosine distance function for feature vectors.
 * <p>
 * The arc cosine distance is computed as the arcus from the cosine similarity
 * value, i.e., <code>arccos(&lt;v1,v2&gt;)</code>.
 * <p>
 * Cosine similarity is defined as
 * \[ \tfrac{\vec{x}\cdot\vec{y}}{||a||\cdot||b||}
 * =_{||a||=||b||=1} \vec{x}\cdot\vec{y} \]
 * Arcus cosine distance then is
 * \[ \operatorname{arccos} \tfrac{\vec{x}\cdot\vec{y}}{||a||\cdot||b||}
 * =_{||a||=||b||=1} \operatorname{arccos} \vec{x}\cdot\vec{y} \in [0;\pi] \]
 * <p>
 * This implementation <em>assumes</em> that \(||a||=||b||=1\). If this does not
 * hold for your data, use {@link ArcCosineDistance} instead!
 * <p>
 * {@link CosineUnitlengthDistance} and {@link SqrtCosineUnitlengthDistance} are
 * a bit less expensive to compute, and will yield the same ranking of
 * neighbors.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ArcCosineUnitlengthDistance implements SpatialPrimitiveDistance<NumberVector>, NumberVectorDistance<NumberVector> {
  /**
   * Static instance
   */
  public static final ArcCosineUnitlengthDistance STATIC = new ArcCosineUnitlengthDistance();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public ArcCosineUnitlengthDistance() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final double v = VectorUtil.dot(v1, v2);
    return v < 1 ? (v > -1 ? Math.acos(v) : 1) : 0;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final double v = VectorUtil.minDot(mbr1, mbr2);
    return v < 1 ? (v > -1 ? Math.acos(v) : 1) : 0;
  }

  @Override
  public String toString() {
    return "ArcCosineUnitlengthDistance";
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
  public static class Par implements Parameterizer {
    @Override
    public ArcCosineUnitlengthDistance make() {
      return ArcCosineUnitlengthDistance.STATIC;
    }
  }
}
