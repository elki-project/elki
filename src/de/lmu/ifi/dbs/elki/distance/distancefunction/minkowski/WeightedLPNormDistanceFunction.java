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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;

/**
 * Weighted version of the Minkowski L_p metrics distance function.
 * 
 * @author Erich Schubert
 */
public class WeightedLPNormDistanceFunction extends LPNormDistanceFunction {
  /**
   * Weight array
   */
  protected double[] weights;

  /**
   * Constructor.
   * 
   * @param p p value
   * @param weights Weight vector
   */
  public WeightedLPNormDistanceFunction(double p, double[] weights) {
    super(p);
    this.weights = weights;
  }

  @Override
  protected double doublePreDistance(NumberVector<?> v1, NumberVector<?> v2, final int start, final int end, double agg) {
    for (int d = start; d < end; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      final double delta = (xd >= yd) ? xd - yd : yd - xd;
      agg += Math.pow(delta, p) * weights[d];
    }
    return agg;
  }

  @Override
  protected double doublePreDistanceVM(NumberVector<?> v, SpatialComparable mbr, final int start, final int end, double agg) {
    for (int d = start; d < end; d++) {
      final double value = v.doubleValue(d), min = mbr.getMin(d);
      double delta = min - value;
      if (delta < 0.) {
        delta = value - mbr.getMax(d);
      }
      if (delta > 0.) {
        agg += Math.pow(delta, p) * weights[d];
      }
    }
    return agg;
  }

  @Override
  protected double doublePreDistanceMBR(SpatialComparable mbr1, SpatialComparable mbr2, final int start, final int end, double agg) {
    for (int d = start; d < end; d++) {
      double delta = mbr2.getMin(d) - mbr1.getMax(d);
      if (delta < 0.) {
        delta = mbr1.getMin(d) - mbr2.getMax(d);
      }
      if (delta > 0.) {
        agg += Math.pow(delta, p) * weights[d];
      }
    }
    return agg;
  }

  @Override
  protected double doublePreNorm(NumberVector<?> v, final int start, final int end, double agg) {
    for (int d = start; d < end; d++) {
      final double xd = v.doubleValue(d);
      final double delta = xd >= 0. ? xd : -xd;
      agg += Math.pow(delta, p) * weights[d];
    }
    return agg;
  }

  @Override
  protected double doublePreNormMBR(SpatialComparable mbr, final int start, final int end, double agg) {
    for (int d = start; d < end; d++) {
      double delta = mbr.getMin(d);
      if (delta < 0.) {
        delta = -mbr.getMax(d);
      }
      if (delta > 0.) {
        agg += Math.pow(delta, p) * weights[d];
      }
    }
    return agg;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof WeightedLPNormDistanceFunction)) {
      if (obj instanceof LPNormDistanceFunction && super.equals(obj)) {
        for (double d : weights) {
          if (d != 1.) {
            return false;
          }
        }
        return true;
      }
      return false;
    }
    WeightedLPNormDistanceFunction other = (WeightedLPNormDistanceFunction) obj;
    return Arrays.equals(this.weights, other.weights);
  }

  @Override
  public SimpleTypeInformation<? super NumberVector<?>> getInputTypeRestriction() {
    return new VectorFieldTypeInformation<>(NumberVector.class, 0, weights.length);
  }
}
