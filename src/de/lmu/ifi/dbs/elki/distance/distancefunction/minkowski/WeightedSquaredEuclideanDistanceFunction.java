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
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractSpatialDoubleDistanceNorm;

/**
 * Provides the squared Euclidean distance for FeatureVectors. This results in
 * the same rankings, but saves computing the square root as often.
 * 
 * @author Arthur Zimek
 */
public class WeightedSquaredEuclideanDistanceFunction extends AbstractSpatialDoubleDistanceNorm {
  /**
   * Weight array
   */
  protected double[] weights;

  /**
   * Constructor.
   * 
   * @param weights
   */
  public WeightedSquaredEuclideanDistanceFunction(double[] weights) {
    super();
    this.weights = weights;
  }

  /**
   * Provides the squared Euclidean distance between the given two vectors.
   * 
   * @return the squared Euclidean distance between the given two vectors as raw
   *         double value
   */
  @Override
  public double doubleDistance(NumberVector<?> v1, NumberVector<?> v2) {
    final int dim = dimensionality(v1, v2, weights.length);
    double agg = 0.;
    for (int d = 0; d < dim; d++) {
      final double delta = (v1.doubleValue(d) - v2.doubleValue(d));
      agg += delta * delta * weights[d];
    }
    return agg;
  }

  @Override
  public double doubleNorm(NumberVector<?> obj) {
    final int dim = obj.getDimensionality();
    double agg = 0.;
    for (int d = 0; d < dim; d++) {
      final double delta = obj.doubleValue(dim);
      agg += delta * delta * weights[d];
    }
    return agg;
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    // Optimization for the simplest case
    if (mbr1 instanceof NumberVector) {
      if (mbr2 instanceof NumberVector) {
        return doubleDistance((NumberVector<?>) mbr1, (NumberVector<?>) mbr2);
      }
    }
    // TODO: optimize for more simpler cases: obj vs. rect?
    final int dim = dimensionality(mbr1, mbr2, weights.length);
    double agg = 0;
    for (int d = 0; d < dim; d++) {
      final double diff;
      if (mbr1.getMax(d) < mbr2.getMin(d)) {
        diff = mbr2.getMin(d) - mbr1.getMax(d);
      } else if (mbr1.getMin(d) > mbr2.getMax(d)) {
        diff = mbr1.getMin(d) - mbr2.getMax(d);
      } else { // The mbrs intersect!
        continue;
      }
      agg += diff * diff * weights[d];
    }
    return agg;
  }

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof WeightedSquaredEuclideanDistanceFunction)) {
      if (obj.getClass().equals(SquaredEuclideanDistanceFunction.class)) {
        for (double d : weights) {
          if (d != 1.0) {
            return false;
          }
        }
        return true;
      }
      return false;
    }
    WeightedSquaredEuclideanDistanceFunction other = (WeightedSquaredEuclideanDistanceFunction) obj;
    return Arrays.equals(this.weights, other.weights);
  }
}
