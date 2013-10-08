package de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Manhattan distance function to compute the Manhattan distance for a pair of
 * FeatureVectors.
 * 
 * @author Arthur Zimek
 */
@Alias({ "taxicab", "cityblock", "l1", "ManhattanDistanceFunction", "de.lmu.ifi.dbs.elki.distance.distancefunction.ManhattanDistanceFunction" })
public class ManhattanDistanceFunction extends LPNormDistanceFunction {
  /**
   * The static instance to use.
   */
  public static final ManhattanDistanceFunction STATIC = new ManhattanDistanceFunction();

  /**
   * Provides a Manhattan distance function that can compute the Manhattan
   * distance (that is a DoubleDistance) for FeatureVectors.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public ManhattanDistanceFunction() {
    super(1.);
  }

  @Override
  public double doubleDistance(NumberVector<?> v1, NumberVector<?> v2) {
    final int dim = dimensionality(v1, v2);
    double agg = 0.;
    for (int d = 0; d < dim; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      final double val = (xd >= yd) ? xd - yd : yd - xd;
      agg += val;
    }
    return agg;
  }

  @Override
  public double doubleNorm(NumberVector<?> v) {
    final int dim = v.getDimensionality();
    double agg = 0.;
    for (int d = 0; d < dim; d++) {
      final double xd = v.doubleValue(d);
      agg += (xd >= 0.) ? xd : -xd;
    }
    return agg;
  }

  protected double doubleMinDistObject(NumberVector<?> v, SpatialComparable mbr) {
    final int dim = dimensionality(mbr, v);
    double agg = 0.;
    for (int d = 0; d < dim; d++) {
      final double value = v.doubleValue(d), min = mbr.getMin(d);
      if (value < min) {
        agg += min - value;
      } else {
        final double max = mbr.getMax(d);
        if (value > max) {
          agg += value - max;
        }
      }
    }
    return agg;
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    // Some optimizations for simpler cases.
    if (mbr1 instanceof NumberVector) {
      if (mbr2 instanceof NumberVector) {
        return doubleDistance((NumberVector<?>) mbr1, (NumberVector<?>) mbr2);
      } else {
        return doubleMinDistObject((NumberVector<?>) mbr1, mbr2);
      }
    } else if (mbr2 instanceof NumberVector) {
      return doubleMinDistObject((NumberVector<?>) mbr2, mbr1);
    }
    final int dim = dimensionality(mbr1, mbr2);

    double agg = 0.;
    for (int d = 0; d < dim; d++) {
      final double d1 = mbr2.getMin(d) - mbr1.getMax(d);
      if (d1 > 0.) {
        agg += d1;
      } else {
        final double d2 = mbr1.getMin(d) - mbr2.getMax(d);
        if (d2 > 0.) {
          agg += d2;
        }
      }
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
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (this.getClass().equals(obj.getClass())) {
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
    protected ManhattanDistanceFunction makeInstance() {
      return ManhattanDistanceFunction.STATIC;
    }
  }
}
