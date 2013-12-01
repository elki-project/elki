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
public class ManhattanDistanceFunction extends LPIntegerNormDistanceFunction {
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
    super(1);
  }

  @Override
  protected double doublePreDistance(NumberVector<?> v1, NumberVector<?> v2, int start, int end, double agg) {
    for (int d = start; d < end; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      final double delta = (xd >= yd) ? xd - yd : yd - xd;
      agg += delta;
    }
    return agg;
  }

  @Override
  protected double doublePreDistanceVM(NumberVector<?> v, SpatialComparable mbr, int start, int end, double agg) {
    for (int d = start; d < end; d++) {
      final double value = v.doubleValue(d), min = mbr.getMin(d);
      double delta = min - value;
      if (delta < 0.) {
        delta = value - mbr.getMax(d);
      }
      if (delta > 0.) {
        agg += delta;
      }
    }
    return agg;
  }

  @Override
  protected double doublePreDistanceMBR(SpatialComparable mbr1, SpatialComparable mbr2, int start, int end, double agg) {
    for (int d = start; d < end; d++) {
      double delta = mbr2.getMin(d) - mbr1.getMax(d);
      if (delta < 0.) {
        delta = mbr1.getMin(d) - mbr2.getMax(d);
      }
      if (delta > 0.) {
        agg += delta;
      }
    }
    return agg;
  }

  @Override
  protected double doublePreNorm(NumberVector<?> v, int start, int end, double agg) {
    for (int d = start; d < end; d++) {
      final double xd = v.doubleValue(d);
      final double delta = (xd >= 0.) ? xd : -xd;
      agg += delta;
    }
    return agg;
  }

  @Override
  protected double doublePreNormMBR(SpatialComparable mbr, int start, int end, double agg) {
    for (int d = start; d < end; d++) {
      double delta = mbr.getMin(d);
      if (delta < 0.) {
        delta = -mbr.getMax(d);
      }
      if (delta > 0.) {
        agg += delta;
      }
    }
    return agg;
  }

  @Override
  protected double finalScale(double agg) {
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
