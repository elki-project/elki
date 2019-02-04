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
package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.math.PearsonCorrelation;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

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
public class SquaredPearsonCorrelationDistanceFunction extends AbstractNumberVectorDistanceFunction {
  /**
   * Static instance.
   */
  public static final SquaredPearsonCorrelationDistanceFunction STATIC = new SquaredPearsonCorrelationDistanceFunction();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated use static instance!
   */
  @Deprecated
  public SquaredPearsonCorrelationDistanceFunction() {
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
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected SquaredPearsonCorrelationDistanceFunction makeInstance() {
      return SquaredPearsonCorrelationDistanceFunction.STATIC;
    }
  }
}
