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
package elki.distance.correlation;

import elki.data.NumberVector;
import elki.distance.AbstractNumberVectorDistance;
import elki.math.PearsonCorrelation;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Squared Pearson correlation distance function for feature vectors.
 * <p>
 * The squared Pearson correlation distance is computed from the
 * Pearson correlation coefficient \(r\) as: \(1-r^2\).
 * Hence, possible values of this distance are between 0 and 1.
 * <p>
 * The distance between two vectors will be low (near 0), if their attribute
 * values are dimension-wise strictly positively or negatively correlated.
 * For features with uncorrelated attributes, the distance value will be high
 * (near 1).
 * 
 * @author Arthur Zimek
 * @since 0.3
 */
public class SquaredPearsonCorrelationDistance extends AbstractNumberVectorDistance {
  /**
   * Static instance.
   */
  public static final SquaredPearsonCorrelationDistance STATIC = new SquaredPearsonCorrelationDistance();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated use static instance!
   */
  @Deprecated
  public SquaredPearsonCorrelationDistance() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final double pcc = PearsonCorrelation.coefficient(v1, v2);
    return 1 - pcc * pcc;
  }

  @Override
  public boolean isSquared() {
    return true;
  }

  @Override
  public String toString() {
    return "SquaredPearsonCorrelationDistance";
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null && this.getClass().equals(obj.getClass());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public SquaredPearsonCorrelationDistance make() {
      return SquaredPearsonCorrelationDistance.STATIC;
    }
  }
}
