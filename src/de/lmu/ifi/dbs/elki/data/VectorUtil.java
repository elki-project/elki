package de.lmu.ifi.dbs.elki.data;

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

import java.util.BitSet;
import java.util.Random;

import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Utility functions for use with vectors.
 * 
 * Note: obviously, many functions are class methods or database related.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.data.NumberVector
 */
public final class VectorUtil {
  /**
   * Return the range across all dimensions. Useful in particular for time series.
   * 
   * @param vec Vector to process.
   * @param <V> Vector type 
   * @return [min, max]
   */
  public static <V extends NumberVector<?, ?>> DoubleMinMax getRangeDouble(V vec) {
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
    return template.newInstance(MathUtil.randomDoubleArray(template.getDimensionality(), r));
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
   * Compute the angle between two vectors.
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @return Angle
   */
  public static double angle(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    if(v1 instanceof SparseNumberVector<?, ?> && v2 instanceof SparseNumberVector<?, ?>) {
      return angleSparse((SparseNumberVector<?, ?>) v1, (SparseNumberVector<?, ?>) v2);
    }
    // TODO: implement without creating Vector instances!
    return MathUtil.angle(v1.getColumnVector(), v2.getColumnVector());
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
}