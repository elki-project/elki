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
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.Norm;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Minimum distance for {@link NumberVector}s.
 * <p>
 * Minimum distance is defined as:
 * \[ \text{Minimum}_p(\vec{x},\vec{y}) := \min_i |x_i-y_i| \]
 * <p>
 * This is not a metric, but can sometimes be useful as a lower bound.
 *
 * @author Erich Schubert
 * @since 0.3
 */
@Alias({ "minimum", "min", "de.lmu.ifi.dbs.elki.distance.distancefunction.MinimumDistanceFunction" })
public class MinimumDistanceFunction extends AbstractNumberVectorDistanceFunction implements SpatialPrimitiveDistanceFunction<NumberVector>, Norm<NumberVector> {
  /**
   * Static instance. Use this.
   */
  public static final MinimumDistanceFunction STATIC = new MinimumDistanceFunction();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public MinimumDistanceFunction() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2);
    double agg = Double.POSITIVE_INFINITY;
    for(int d = 0; d < dim; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      final double val = xd >= yd ? xd - yd : yd - xd;
      agg = val < agg ? val : agg;
    }
    return agg;
  }

  @Override
  public double norm(NumberVector v) {
    final int dim = v.getDimensionality();
    double agg = Double.POSITIVE_INFINITY;
    for(int d = 0; d < dim; d++) {
      final double xd = v.doubleValue(d);
      final double val = xd >= 0. ? xd : -xd;
      agg = val < agg ? val : agg;
    }
    return agg;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    // Some optimizations for simpler cases.
    if(mbr1 instanceof NumberVector && mbr2 instanceof NumberVector) {
      return distance((NumberVector) mbr1, (NumberVector) mbr2);
    }
    // TODO: add optimization for point to MBR?
    final int dim = dimensionality(mbr1, mbr2);
    double agg = Double.POSITIVE_INFINITY;
    for(int d = 0; d < dim; d++) {
      double diff = mbr2.getMin(d) - mbr1.getMax(d);
      if(diff <= 0.) {
        diff = mbr1.getMin(d) - mbr2.getMax(d);
        if(diff <= 0.) {
          // The objects overlap in this dimension.
          return 0.;
        }
      }
      agg = diff > agg ? agg : diff;
    }
    return agg;
  }

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public String toString() {
    return "MinimumDistance";
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
    protected MinimumDistanceFunction makeInstance() {
      return MinimumDistanceFunction.STATIC;
    }
  }
}
