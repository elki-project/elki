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
import elki.utilities.optionhandling.Parameterizer;

/**
 * Absolute uncentered correlation distance function for feature vectors.
 * 
 * This is highly similar to {@link AbsolutePearsonCorrelationDistance},
 * but uses a fixed mean of 0 instead of the sample mean.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class AbsoluteUncenteredCorrelationDistance extends AbstractNumberVectorDistance {
  /**
   * Static instance.
   */
  public static final AbsoluteUncenteredCorrelationDistance STATIC = new AbsoluteUncenteredCorrelationDistance();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public AbsoluteUncenteredCorrelationDistance() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    return 1. - Math.abs(UncenteredCorrelationDistance.uncenteredCorrelation(v1, v2));
  }

  @Override
  public String toString() {
    return "AbsoluteUncenteredCorrelationDistance";
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
    public AbsoluteUncenteredCorrelationDistance make() {
      return AbsoluteUncenteredCorrelationDistance.STATIC;
    }
  }
}