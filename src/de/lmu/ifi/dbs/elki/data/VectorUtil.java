package de.lmu.ifi.dbs.elki.data;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.BitSet;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * Utility functions for use with vectors.
 * 
 * Note: obviously, many functions are class methods or database related.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses NumberVector
 */
public final class VectorUtil {
  /**
   * Return the range across all dimensions. Useful in particular for time
   * series.
   * 
   * @param vec Vector to process.
   * @return [min, max]
   */
  public static DoubleMinMax getRangeDouble(NumberVector<?, ?> vec) {
    DoubleMinMax minmax = new DoubleMinMax();

    for(int i = 0; i < vec.getDimensionality(); i++) {
      minmax.put(vec.doubleValue(i + 1));
    }

    return minmax;
  }

  /**
   * Produce a new vector based on random numbers in [0:1] of the same type and
   * dimensionality as the given vector.
   * 
   * @param template existing instance of wanted dimensionality.
   * @param r Random generator
   * @return new instance
   */
  public static <V extends NumberVector<V, ?>> V randomVector(V template, Random r) {
    return template.newNumberVector(MathUtil.randomDoubleArray(template.getDimensionality(), r));
  }

  /**
   * Produce a new vector based on random numbers in [0:1] of the same type and
   * dimensionality as the given vector.
   * 
   * @param template existing instance of wanted dimensionality.
   * @return new instance
   */
  public static <V extends NumberVector<V, ?>> V randomVector(V template) {
    return randomVector(template, new Random());
  }

  /**
   * Compute the angle for sparse vectors.
   * 
   * @param v1 First vector
   * @param v2 Second vector
   * @return angle
   */
  public static double angleSparse(SparseNumberVector<?, ?> v1, SparseNumberVector<?, ?> v2) {
    BitSet b1 = v1.getNotNullMask();
    BitSet b2 = v2.getNotNullMask();
    BitSet both = (BitSet) b1.clone();
    both.and(b2);

    // Length of first vector
    double l1 = 0.0;
    for(int i = b1.nextSetBit(0); i >= 0; i = b1.nextSetBit(i + 1)) {
      final double val = v1.doubleValue(i);
      l1 += val * val;
    }
    l1 = Math.sqrt(l1);

    // Length of second vector
    double l2 = 0.0;
    for(int i = b2.nextSetBit(0); i >= 0; i = b2.nextSetBit(i + 1)) {
      final double val = v2.doubleValue(i);
      l2 += val * val;
    }
    l2 = Math.sqrt(l2);

    // Cross product
    double cross = 0.0;
    for(int i = both.nextSetBit(0); i >= 0; i = both.nextSetBit(i + 1)) {
      cross += v1.doubleValue(i) * v2.doubleValue(i);
    }
    return cross / (l1 * l2);
  }

  /**
   * Compute the angle between two vectors.
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @param o Origin
   * @return Angle
   */
  public static double angle(NumberVector<?, ?> v1, NumberVector<?, ?> v2, Vector o) {
    // Essentially, we want to compute this:
    // v1' = v1 - o, v2' = v2 - o
    // v1'.transposeTimes(v2') / (v1'.euclideanLength()*v2'.euclideanLength());
    // We can just compute all three in parallel.
    double[] oe = o.getArrayRef();
    final int dim = v1.getDimensionality();
    double s = 0, e1 = 0, e2 = 0;
    for(int k = 0; k < dim; k++) {
      final double r1 = v1.doubleValue(k + 1) - oe[k];
      final double r2 = v2.doubleValue(k + 1) - oe[k];
      s += r1 * r2;
      e1 += r1 * r1;
      e2 += r2 * r2;
    }
    return Math.sqrt((s / e1) * (s / e2));
  }

  /**
   * Compute the angle between two vectors.
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @param o Origin
   * @return Angle
   */
  public static double angle(NumberVector<?, ?> v1, NumberVector<?, ?> v2, NumberVector<?, ?> o) {
    // Essentially, we want to compute this:
    // v1' = v1 - o, v2' = v2 - o
    // v1'.transposeTimes(v2') / (v1'.euclideanLength()*v2'.euclideanLength());
    // We can just compute all three in parallel.
    final int dim = v1.getDimensionality();
    double s = 0, e1 = 0, e2 = 0;
    for(int k = 0; k < dim; k++) {
      final double r1 = v1.doubleValue(k + 1) - o.doubleValue(k + 1);
      final double r2 = v2.doubleValue(k + 1) - o.doubleValue(k + 1);
      s += r1 * r2;
      e1 += r1 * r1;
      e2 += r2 * r2;
    }
    return Math.sqrt((s / e1) * (s / e2));
  }

  /**
   * Compute the absolute cosine of the angle between two vectors.
   * 
   * To convert it to radians, use <code>Math.acos(angle)</code>!
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @return Angle
   */
  public static double cosAngle(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    if(v1 instanceof SparseNumberVector<?, ?> && v2 instanceof SparseNumberVector<?, ?>) {
      return angleSparse((SparseNumberVector<?, ?>) v1, (SparseNumberVector<?, ?>) v2);
    }
    // Essentially, we want to compute this:
    // v1.transposeTimes(v2) / (v1.euclideanLength() * v2.euclideanLength());
    // We can just compute all three in parallel.
    final int dim = v1.getDimensionality();
    double s = 0, e1 = 0, e2 = 0;
    for(int k = 0; k < dim; k++) {
      final double r1 = v1.doubleValue(k + 1);
      final double r2 = v2.doubleValue(k + 1);
      s += r1 * r2;
      e1 += r1 * r1;
      e2 += r2 * r2;
    }
    return Math.min(Math.sqrt((s / e1) * (s / e2)), 1);
  }

  // TODO: add more precise but slower O(n^2) angle computation according to:
  // Computing the Angle between Vectors, P. Schatte
  // Journal of Computing, Volume 63, Number 1 (1999)

  /**
   * Compute the minimum angle between two rectangles.
   * 
   * @param v1 first rectangle
   * @param v2 second rectangle
   * @return Angle
   */
  public static double minCosAngle(SpatialComparable v1, SpatialComparable v2) {
    if(v1 instanceof NumberVector<?, ?> && v2 instanceof NumberVector<?, ?>) {
      return cosAngle((NumberVector<?, ?>) v1, (NumberVector<?, ?>) v2);
    }
    // Essentially, we want to compute this:
    // absmax(v1.transposeTimes(v2))/(min(v1.euclideanLength())*min(v2.euclideanLength()));
    // We can just compute all three in parallel.
    final int dim = v1.getDimensionality();
    double s1 = 0, s2 = 0, e1 = 0, e2 = 0;
    for(int k = 0; k < dim; k++) {
      final double min1 = v1.getMin(k + 1), max1 = v1.getMax(k + 1);
      final double min2 = v2.getMin(k + 1), max2 = v2.getMax(k + 1);
      final double p1 = min1 * min2, p2 = min1 * max2;
      final double p3 = max1 * min2, p4 = max1 * max2;
      s1 += Math.max(Math.max(p1, p2), Math.max(p3, p4));
      s2 += Math.min(Math.min(p1, p2), Math.min(p3, p4));
      if(max1 < 0) {
        e1 += max1 * max1;
      }
      else if(min1 > 0) {
        e1 += min1 * min1;
      } // else: 0
      if(max2 < 0) {
        e2 += max2 * max2;
      }
      else if(min2 > 0) {
        e2 += min2 * min2;
      } // else: 0
    }
    final double s = Math.max(s1, Math.abs(s2));
    return Math.min(Math.sqrt((s / e1) * (s / e2)), 1.0);
  }

  /**
   * Provides the scalar product (inner product) of this and the given
   * DoubleVector.
   * 
   * @param d1 the first vector to compute the scalar product for
   * @param d2 the second vector to compute the scalar product for
   * @return the scalar product (inner product) of this and the given
   *         DoubleVector
   */
  public static double scalarProduct(NumberVector<?, ?> d1, NumberVector<?, ?> d2) {
    final int dim = d1.getDimensionality();
    double result = 0.0;
    for(int i = 1; i <= dim; i++) {
      result += d1.doubleValue(i) * d2.doubleValue(i);
    }
    return result;
  }
}