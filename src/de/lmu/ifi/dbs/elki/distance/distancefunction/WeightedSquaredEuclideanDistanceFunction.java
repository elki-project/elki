package de.lmu.ifi.dbs.elki.distance.distancefunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

/**
 * Provides the squared Euclidean distance for FeatureVectors. This results in
 * the same rankings, but saves computing the square root as often.
 * 
 * @author Arthur Zimek
 */
public class WeightedSquaredEuclideanDistanceFunction extends AbstractVectorDoubleDistanceFunction {
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
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    final int dim1 = v1.getDimensionality();
    if(dim1 != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString() + "\n" + v1.getDimensionality() + "!=" + v2.getDimensionality());
    }
    double sqrDist = 0;
    for(int i = 1; i <= dim1; i++) {
      final double delta = v1.doubleValue(i) - v2.doubleValue(i);
      sqrDist += delta * delta * weights[i - 1];
    }
    return sqrDist;
  }

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(!(obj instanceof WeightedSquaredEuclideanDistanceFunction)) {
      if(obj.getClass().equals(SquaredEuclideanDistanceFunction.class)) {
        for(double d : weights) {
          if(d != 1.0) {
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