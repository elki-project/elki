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
package de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Maximum distance for {@link NumberVector}s.
 * <p>
 * The maximum distance is defined as:
 * \[ \text{Maximum}(\vec{x},\vec{y}) := \max_i |x_i-y_i| \]
 * and can be seen as limiting case of the {@link LPNormDistanceFunction}
 * for \( p \rightarrow \infty \).
 *
 * @author Erich Schubert
 * @since 0.3
 */
@Alias({ "maximum", "max", "chebyshev", "de.lmu.ifi.dbs.elki.distance.distancefunction.MaximumDistanceFunction" })
public class MaximumDistanceFunction extends LPNormDistanceFunction {
  /**
   * Static instance.
   */
  public static final MaximumDistanceFunction STATIC = new MaximumDistanceFunction();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public MaximumDistanceFunction() {
    super(Double.POSITIVE_INFINITY);
  }

  private double preDistance(NumberVector v1, NumberVector v2, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      final double delta = xd >= yd ? xd - yd : yd - xd;
      agg = delta >= agg ? delta : agg;
    }
    return agg;
  }

  private double preDistanceVM(NumberVector v, SpatialComparable mbr, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double value = v.doubleValue(d), min = mbr.getMin(d);
      double delta = min - value;
      delta = delta >= 0 ? delta : value - mbr.getMax(d);
      agg = delta >= agg ? delta : agg;
    }
    return agg;
  }

  private double preDistanceMBR(SpatialComparable mbr1, SpatialComparable mbr2, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      double delta = mbr2.getMin(d) - mbr1.getMax(d);
      delta = delta >= 0 ? delta : mbr1.getMin(d) - mbr2.getMax(d);
      agg = delta >= agg ? delta : agg;
    }
    return agg;
  }

  private double preNorm(NumberVector v, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double xd = v.doubleValue(d);
      final double delta = xd >= 0. ? xd : -xd;
      agg = delta >= agg ? delta : agg;
    }
    return agg;
  }

  private double preNormMBR(SpatialComparable mbr, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      double delta = mbr.getMin(d);
      delta = delta >= 0 ? delta : -mbr.getMax(d);
      agg = delta >= agg ? delta : agg;
    }
    return agg;
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim1 = v1.getDimensionality(), dim2 = v2.getDimensionality();
    final int mindim = dim1 < dim2 ? dim1 : dim2;
    double agg = preDistance(v1, v2, 0, mindim);
    if(dim1 > mindim) {
      double b = preNorm(v1, mindim, dim1);
      agg = agg >= b ? agg : b;
    }
    else if(dim2 > mindim) {
      double b = preNorm(v2, mindim, dim2);
      agg = agg >= b ? agg : b;
    }
    return agg;
  }

  @Override
  public double norm(NumberVector v) {
    return preNorm(v, 0, v.getDimensionality());
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim1 = mbr1.getDimensionality(), dim2 = mbr2.getDimensionality();
    final int mindim = dim1 < dim2 ? dim1 : dim2;

    final NumberVector v1 = (mbr1 instanceof NumberVector) ? (NumberVector) mbr1 : null;
    final NumberVector v2 = (mbr2 instanceof NumberVector) ? (NumberVector) mbr2 : null;

    double agg = (v1 != null) //
        ? (v2 != null) ? preDistance(v1, v2, 0, mindim) : preDistanceVM(v1, mbr2, 0, mindim) //
        : (v2 != null) ? preDistanceVM(v2, mbr1, 0, mindim) : preDistanceMBR(mbr1, mbr2, 0, mindim);
    // first object has more dimensions.
    if(dim1 > mindim) {
      double b = (v1 != null) ? preNorm(v1, mindim, dim1) : preNormMBR(mbr1, mindim, dim1);
      agg = agg >= b ? agg : b;
    }
    // second object has more dimensions.
    if(dim2 > mindim) {
      double b = (v2 != null) ? preNorm(v2, mindim, dim2) : preNormMBR(mbr2, mindim, dim2);
      agg = agg >= b ? agg : b;
    }
    return agg;
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public String toString() {
    return "MaximumDistance";
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj != null && this.getClass().equals(obj.getClass()));
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
    protected MaximumDistanceFunction makeInstance() {
      return MaximumDistanceFunction.STATIC;
    }
  }
}
