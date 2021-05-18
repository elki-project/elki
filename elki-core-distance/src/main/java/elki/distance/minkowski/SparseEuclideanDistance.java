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
package elki.distance.minkowski;

import elki.data.SparseNumberVector;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Euclidean distance function, optimized for {@link SparseNumberVector}s.
 * <p>
 * Euclidean distance is defined as:
 * \[ \text{Euclidean}(\vec{x},\vec{y}) := \sqrt{\sum\nolimits_i (x_i-y_i)^2} \]
 * <p>
 * For sparse vectors, we can skip those i where both vectors are 0.
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
public class SparseEuclideanDistance extends SparseLPNormDistance {
  /**
   * Static instance
   */
  public static final SparseEuclideanDistance STATIC = new SparseEuclideanDistance();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance instead.
   */
  @Deprecated
  public SparseEuclideanDistance() {
    super(2.);
  }

  @Override
  public double distance(SparseNumberVector v1, SparseNumberVector v2) {
    // Get the bit masks
    double accu = 0.;
    int i1 = v1.iter(), i2 = v2.iter();
    while(v1.iterValid(i1) && v2.iterValid(i2)) {
      final int d1 = v1.iterDim(i1), d2 = v2.iterDim(i2);
      if(d1 < d2) {
        // In first only
        final double val = v1.iterDoubleValue(i1);
        accu += val * val;
        i1 = v1.iterAdvance(i1);
      }
      else if(d2 < d1) {
        // In second only
        final double val = v2.iterDoubleValue(i2);
        accu += val * val;
        i2 = v2.iterAdvance(i2);
      }
      else {
        // Both vectors have a value.
        final double val = v1.iterDoubleValue(i1) - v2.iterDoubleValue(i2);
        accu += val * val;
        i1 = v1.iterAdvance(i1);
        i2 = v2.iterAdvance(i2);
      }
    }
    while(v1.iterValid(i1)) {
      // In first only
      final double val = v1.iterDoubleValue(i1);
      accu += val * val;
      i1 = v1.iterAdvance(i1);
    }
    while(v2.iterValid(i2)) {
      // In second only
      final double val = v2.iterDoubleValue(i2);
      accu += val * val;
      i2 = v2.iterAdvance(i2);
    }
    return Math.sqrt(accu);
  }

  @Override
  public double norm(SparseNumberVector v1) {
    double accu = 0.;
    for(int it = v1.iter(); v1.iterValid(it); it = v1.iterAdvance(it)) {
      final double val = v1.iterDoubleValue(it);
      accu += val * val;
    }
    return Math.sqrt(accu);
  }

  /**
   * Parameterizer
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public SparseEuclideanDistance make() {
      return SparseEuclideanDistance.STATIC;
    }
  }
}
