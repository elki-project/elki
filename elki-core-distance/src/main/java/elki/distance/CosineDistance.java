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
import elki.utilities.Alias;
import elki.utilities.Priority;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Cosine distance function for feature vectors.
 * <p>
 * The cosine distance is computed from the cosine similarity by
 * <code>1-(cosine similarity)</code>.
 * <p>
 * Cosine similarity is defined as
 * \[ \tfrac{\vec{x}\cdot\vec{y}}{||a||\cdot||b||} \]
 * Cosine distance then is defined as
 * \[ 1 - \tfrac{\vec{x}\cdot\vec{y}}{||a||\cdot||b||} \in [0;2] \]
 * <p>
 * {@link ArcCosineDistance} or {@link SqrtCosineDistance} can be used if you
 * need a metric, but are more expensive to computate.
 *
 * @author Arthur Zimek
 * @since 0.1
 */
@Priority(Priority.IMPORTANT)
@Alias({ "cosine" })
public class CosineDistance implements SpatialPrimitiveDistance<NumberVector>, NumberVectorDistance<NumberVector> {
  /**
   * Static instance
   */
  public static final CosineDistance STATIC = new CosineDistance();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance
   */
  @Deprecated
  public CosineDistance() {
    super();
  }

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
  public boolean isSquared() {
    return true;
  }

  @Override
  public String toString() {
    return "CosineDistance";
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
    public CosineDistance make() {
      return CosineDistance.STATIC;
    }
  }
}
