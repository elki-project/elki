package de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractSpatialNorm;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Maximum distance for {@link NumberVector}s.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
@Alias({ "minimum", "min", "de.lmu.ifi.dbs.elki.distance.distancefunction.MinimumDistanceFunction" })
public class MinimumDistanceFunction extends AbstractSpatialNorm {
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
      final double val = (xd >= yd) ? xd - yd : yd - xd;
      if(val < agg) {
        agg = val;
      }
    }
    return agg;
  }

  @Override
  public double norm(NumberVector v) {
    final int dim = v.getDimensionality();
    double agg = Double.POSITIVE_INFINITY;
    for(int d = 0; d < dim; d++) {
      final double xd = v.doubleValue(d);
      final double val = (xd >= 0.) ? xd : -xd;
      if(val < agg) {
        agg = val;
      }
    }
    return agg;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    // Some optimizations for simpler cases.
    if(mbr1 instanceof NumberVector) {
      if(mbr2 instanceof NumberVector) {
        return distance((NumberVector) mbr1, (NumberVector) mbr2);
      }
    }
    // TODO: add optimization for point to MBR?
    final int dim = dimensionality(mbr1, mbr2);
    double agg = Double.POSITIVE_INFINITY;
    for(int d = 0; d < dim; d++) {
      final double diff;
      final double d1 = mbr2.getMin(d) - mbr1.getMax(d);
      if(d1 > 0.) {
        diff = d1;
      }
      else {
        final double d2 = mbr1.getMin(d) - mbr2.getMax(d);
        if(d2 > 0.) {
          diff = d2;
        }
        else {
          // The objects overlap in this dimension.
          return 0.;
        }
      }
      if(diff < agg) {
        agg = diff;
      }
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
    if(obj == null) {
      return false;
    }
    if(obj == this) {
      return true;
    }
    if(this.getClass().equals(obj.getClass())) {
      return true;
    }
    return super.equals(obj);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected MinimumDistanceFunction makeInstance() {
      return MinimumDistanceFunction.STATIC;
    }
  }
}
