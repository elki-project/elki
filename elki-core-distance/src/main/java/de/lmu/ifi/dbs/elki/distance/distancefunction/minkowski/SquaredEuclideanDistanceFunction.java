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
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.*;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Squared Euclidean distance, optimized for {@link SparseNumberVector}s. This
 * results in the same rankings as regular Euclidean distance, but saves
 * computing the square root.
 * <p>
 * Squared Euclidean is defined as:
 * \[ \text{Euclidean}^2(\vec{x},\vec{y}) := \sum_i (x_i-y_i)^2 \]
 *
 * @author Arthur Zimek
 * @since 0.1
 */
@Alias({ "squaredeuclidean", "de.lmu.ifi.dbs.elki.distance.distancefunction.SquaredEuclideanDistanceFunction" })
public class SquaredEuclideanDistanceFunction extends AbstractNumberVectorDistanceFunction implements SpatialPrimitiveDistanceFunction<NumberVector>, Norm<NumberVector> {
  /**
   * Static instance. Use this!
   */
  public static final SquaredEuclideanDistanceFunction STATIC = new SquaredEuclideanDistanceFunction();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public SquaredEuclideanDistanceFunction() {
    super();
  }

  private double preDistance(double[] v1, double[] v2, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double delta = v1[d] - v2[d];
      agg += delta * delta;
    }
    return agg;
  }

  private double preDistance(NumberVector v1, NumberVector v2, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double delta = v1.doubleValue(d) - v2.doubleValue(d);
      agg += delta * delta;
    }
    return agg;
  }

  private double preDistanceVM(NumberVector v, SpatialComparable mbr, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double value = v.doubleValue(d);
      double delta = mbr.getMin(d) - value;
      delta = delta >= 0. ? delta : value - mbr.getMax(d);
      if(delta > 0.) {
        agg += delta * delta;
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
        agg += delta * delta;
      }
    }
    return agg;
  }

  private double preNorm(NumberVector v, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double xd = v.doubleValue(d);
      agg += xd * xd;
    }
    return agg;
  }

  private double preNorm(double[] v, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      final double xd = v[d];
      agg += xd * xd;
    }
    return agg;
  }

  private double preNormMBR(SpatialComparable mbr, int start, int end) {
    double agg = 0.;
    for(int d = start; d < end; d++) {
      double delta = mbr.getMin(d);
      delta = delta >= 0. ? delta : -mbr.getMax(d);
      if(delta > 0.) {
        agg += delta * delta;
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

  /**
   * Special version for double arrays.
   */
  public double distance(double[] v1, double[] v2) {
    final int dim1 = v1.length, dim2 = v2.length;
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
  public boolean isSquared() {
    return true;
  }

  @Override
  public String toString() {
    return "SquaredEuclideanDistance";
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj != null && this.getClass().equals(obj.getClass()));
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return NumberVector.VARIABLE_LENGTH;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected SquaredEuclideanDistanceFunction makeInstance() {
      return SquaredEuclideanDistanceFunction.STATIC;
    }
  }
}
