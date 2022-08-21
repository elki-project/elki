/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
import elki.utilities.optionhandling.Parameterizer;

/**
 * Cosine distance function for feature vectors using the square root.
 * <p>
 * The cosine distance is computed from the cosine similarity by
 * <code>sqrt(1-cosine similarity)</code>.
 * <p>
 * Cosine similarity is defined as
 * \[ \tfrac{\vec{x}\cdot\vec{y}}{||a||\cdot||b||} \]
 * This versions of cosine distance then is defined as
 * \[ \sqrt{2 - 2\tfrac{\vec{x}\cdot\vec{y}}{||a||\cdot||b||}} \in [0;2] \]
 * <p>
 * Because of the square root, this is more expensive than regular cosine,
 * but because this corresponds to Euclidean distance on normalized vectors,
 * it is a metric on normalized vectors.
 *
 * @author Erich Schubert
 * @since 0.1
 */
public class SqrtCosineDistance extends CosineDistance {
  /**
   * Static instance
   */
  public static final SqrtCosineDistance STATIC = new SqrtCosineDistance();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance
   */
  @Deprecated
  public SqrtCosineDistance() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    double d = VectorUtil.cosAngle(v1, v2);
    return (d <= 1) ? Math.sqrt(2 - 2 * d) : 0;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    double d = VectorUtil.minCosAngle(mbr1, mbr2);
    return (d <= 1) ? Math.sqrt(2 - 2 * d) : 0;
  }

  @Override
  public String toString() {
    return "SqrtCosineDistance";
  }

  @Override
  public boolean isSquared() {
    return false;
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public SqrtCosineDistance make() {
      return SqrtCosineDistance.STATIC;
    }
  }
}
