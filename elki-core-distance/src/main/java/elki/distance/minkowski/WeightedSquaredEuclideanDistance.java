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
package elki.distance.minkowski;

import java.util.Arrays;

import elki.data.NumberVector;
import elki.data.spatial.SpatialComparable;
import elki.distance.AbstractNumberVectorDistance;
import elki.distance.Norm;
import elki.distance.SpatialPrimitiveDistance;
import elki.distance.WeightedNumberVectorDistance;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleListParameter;

/**
 * Weighted squared Euclidean distance for {@link NumberVector}s. This results
 * in the same rankings as weighted Euclidean distance, but saves computing the
 * square root.
 * <p>
 * Weighted squared Euclidean is defined as:
 * \[ \text{Euclidean}^2_{\vec{w}}(\vec{x},\vec{y}) := \sum_i w_i (x_i-y_i)^2 \]
 *
 * @author Arthur Zimek
 * @since 0.4.0
 */
public class WeightedSquaredEuclideanDistance extends AbstractNumberVectorDistance implements SpatialPrimitiveDistance<NumberVector>, WeightedNumberVectorDistance<NumberVector>, Norm<NumberVector> {
  /**
   * Weight array
   */
  protected double[] weights;

  /**
   * Constructor.
   * 
   * @param weights Weight vector
   */
  public WeightedSquaredEuclideanDistance(double[] weights) {
    super();
    this.weights = weights;
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2, weights.length);
    double agg = 0.;
    for(int d = 0; d < dim; d++) {
      final double delta = v1.doubleValue(d) - v2.doubleValue(d);
      agg += delta * delta * weights[d];
    }
    return agg;
  }

  @Override
  public double norm(NumberVector obj) {
    final int dim = obj.getDimensionality();
    double agg = 0.;
    for(int d = 0; d < dim; d++) {
      final double delta = obj.doubleValue(dim);
      agg += delta * delta * weights[d];
    }
    return agg;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    // Optimization for the simplest case
    if(mbr1 instanceof NumberVector && mbr2 instanceof NumberVector) {
      return distance((NumberVector) mbr1, (NumberVector) mbr2);
    }
    // TODO: optimize for more simpler cases: obj vs. rect?
    final int dim = dimensionality(mbr1, mbr2, weights.length);
    double agg = 0;
    for(int d = 0; d < dim; d++) {
      final double diff;
      if(mbr1.getMax(d) < mbr2.getMin(d)) {
        diff = mbr2.getMin(d) - mbr1.getMax(d);
      }
      else if(mbr1.getMin(d) > mbr2.getMax(d)) {
        diff = mbr1.getMin(d) - mbr2.getMax(d);
      }
      else { // The mbrs intersect!
        continue;
      }
      agg += diff * diff * weights[d];
    }
    return agg;
  }

  @Override
  public boolean isSquared() {
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(!(obj instanceof WeightedSquaredEuclideanDistance)) {
      if(obj.getClass().equals(SquaredEuclideanDistance.class)) {
        for(double d : weights) {
          if(d != 1.0) {
            return false;
          }
        }
        return true;
      }
      return false;
    }
    WeightedSquaredEuclideanDistance other = (WeightedSquaredEuclideanDistance) obj;
    return Arrays.equals(this.weights, other.weights);
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Weight array
     */
    protected double[] weights;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleListParameter weightsP = new DoubleListParameter(WEIGHTS_ID);
      if(config.grab(weightsP)) {
        weights = weightsP.getValue().clone();
      }
    }

    @Override
    protected WeightedSquaredEuclideanDistance makeInstance() {
      return new WeightedSquaredEuclideanDistance(weights);
    }
  }
}
