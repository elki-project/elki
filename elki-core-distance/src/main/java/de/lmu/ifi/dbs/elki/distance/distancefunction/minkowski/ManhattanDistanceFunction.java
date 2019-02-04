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
 * Manhattan distance for {@link NumberVector}s.
 * <p>
 * Manhattan distance is defined as:
 * \[ \text{Manhattan}(\vec{x},\vec{y}) := \sum_i |x_i-y_i| \]
 * 
 * @author Arthur Zimek
 * @since 0.1
 */
@Alias({ "taxicab", "cityblock", "l1", //
    "de.lmu.ifi.dbs.elki.distance.distancefunction.ManhattanDistanceFunction" })
public class ManhattanDistanceFunction extends LPIntegerNormDistanceFunction {
  /**
   * The static instance to use.
   */
  public static final ManhattanDistanceFunction STATIC = new ManhattanDistanceFunction();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public ManhattanDistanceFunction() {
    super(1);
  }

  private double preDistance(NumberVector v1, NumberVector v2, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      final double delta = xd >= yd ? xd - yd : yd - xd;
      agg += delta;
    }
    return agg;
  }

  private double preDistanceVM(NumberVector v, SpatialComparable mbr, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double value = v.doubleValue(d), min = mbr.getMin(d);
      double delta = min - value;
      delta = delta >= 0. ? delta : value - mbr.getMax(d);
      if(delta > 0.) {
        agg += delta;
      }
    }
    return agg;
  }

  private double preDistanceMBR(SpatialComparable mbr1, SpatialComparable mbr2, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      double delta = mbr2.getMin(d) - mbr1.getMax(d);
      delta = delta >= 0. ? delta : mbr1.getMin(d) - mbr2.getMax(d);
      if(delta > 0.) {
        agg += delta;
      }
    }
    return agg;
  }

  private double preNorm(NumberVector v, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double xd = v.doubleValue(d);
      final double delta = (xd >= 0.) ? xd : -xd;
      agg += delta;
    }
    return agg;
  }

  private double preNormMBR(SpatialComparable mbr, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      double delta = mbr.getMin(d);
      delta = delta >= 0. ? delta : -mbr.getMax(d);
      if(delta > 0.) {
        agg += delta;
      }
    }
    return agg;
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim1 = v1.getDimensionality(), dim2 = v2.getDimensionality();
    final int mindim = dim1 < dim2 ? dim1 : dim2;
    double agg = preDistance(v1, v2, 0, mindim);
    if(dim1 > mindim) {
      agg += preNorm(v1, mindim, dim1);
    }
    else if(dim2 > mindim) {
      agg += preNorm(v2, mindim, dim2);
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
      agg += (v1 != null) ? preNorm(v1, mindim, dim1) : preNormMBR(mbr1, mindim, dim1);
    }
    // second object has more dimensions.
    if(dim2 > mindim) {
      agg += (v2 != null) ? preNorm(v2, mindim, dim2) : preNormMBR(mbr2, mindim, dim2);
    }
    return agg;
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public String toString() {
    return "ManhattanDistance";
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
    protected ManhattanDistanceFunction makeInstance() {
      return ManhattanDistanceFunction.STATIC;
    }
  }
}
