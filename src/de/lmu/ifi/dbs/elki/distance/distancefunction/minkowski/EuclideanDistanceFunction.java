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
 * Provides the Euclidean distance for FeatureVectors.
 * 
 * @author Arthur Zimek
 */
@Alias({ "euclidean", "euclid", "l2", "EuclideanDistanceFunction", "de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction" })
public class EuclideanDistanceFunction extends LPNormDistanceFunction {
  /**
   * Static instance. Use this!
   */
  public static final EuclideanDistanceFunction STATIC = new EuclideanDistanceFunction();

  /**
   * Provides a Euclidean distance function that can compute the Euclidean
   * distance (that is a DoubleDistance) for FeatureVectors.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public EuclideanDistanceFunction() {
    super(2.);
  }

  @Override
  public double doubleDistance(NumberVector<?> v1, NumberVector<?> v2) {
    final int dim = dimensionality(v1, v2);
    double agg = 0.;
    for (int d = 0; d < dim; d++) {
      final double delta = v1.doubleValue(d) - v2.doubleValue(d);
      agg += delta * delta;
    }
    return Math.sqrt(agg);
  }

  @Override
  public double doubleNorm(NumberVector<?> v) {
    final int dim = v.getDimensionality();
    double agg = 0.;
    for (int d = 0; d < dim; d++) {
      final double val = v.doubleValue(d);
      agg += val * val;
    }
    return Math.sqrt(agg);
  }

  protected double doubleMinDistObject(NumberVector<?> v, SpatialComparable mbr) {
    final int dim = dimensionality(mbr, v);
    double agg = 0.;
    for (int d = 0; d < dim; d++) {
      final double value = v.doubleValue(d), min = mbr.getMin(d);
      final double diff;
      if (value < min) {
        diff = min - value;
      } else {
        final double max = mbr.getMax(d);
        if (value > max) {
          diff = value - max;
        } else {
          continue;
        }
      }
      agg += diff * diff;
    }
    return Math.sqrt(agg);
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
      final double diff;
      final double d1 = mbr2.getMin(d) - mbr1.getMax(d);
      if (d1 > 0.) {
        diff = d1;
      } else {
        final double d2 = mbr1.getMin(d) - mbr2.getMax(d);
        if (d2 > 0.) {
          diff = d2;
        } else {
          continue;
        }
      }
      agg += diff * diff;
    }
    return Math.sqrt(agg);
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public String toString() {
    return "EuclideanDistance";
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
    protected EuclideanDistanceFunction makeInstance() {
      return EuclideanDistanceFunction.STATIC;
    }
  }
}
