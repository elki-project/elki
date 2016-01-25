package de.lmu.ifi.dbs.elki.data;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.Comparator;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;

/**
 * Utility functions for use with vectors.
 *
 * Note: obviously, many functions are class methods or database related.
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @apiviz.uses NumberVector
 */
public final class VectorUtil {
  /**
   * Fake constructor. Do not instantiate, use static methods.
   */
  private VectorUtil() {
    // Do not instantiate - utility class.
  }

  /**
   * Produce a new vector based on random numbers in [0:1].
   *
   * @param factory Vector factory
   * @param dim desired dimensionality
   * @param r Random generator
   * @param <V> vector type
   * @return new instance
   */
  public static <V extends NumberVector> V randomVector(NumberVector.Factory<V> factory, int dim, Random r) {
    return factory.newNumberVector(MathUtil.randomDoubleArray(dim, r));
  }

  /**
   * Produce a new vector based on random numbers in [0:1].
   *
   * @param factory Vector factory
   * @param dim desired dimensionality
   * @param <V> vector type
   * @return new instance
   */
  public static <V extends NumberVector> V randomVector(NumberVector.Factory<V> factory, int dim) {
    return randomVector(factory, dim, new Random());
  }

  /**
   * Compute the angle for sparse vectors.
   *
   * @param v1 First vector
   * @param v2 Second vector
   * @return angle
   */
  public static double angleSparse(SparseNumberVector v1, SparseNumberVector v2) {
    // TODO: exploit precomputed length, when available?
    // Length of first vector
    double l1 = 0., l2 = 0., cross = 0.;
    int i1 = v1.iter(), i2 = v2.iter();
    while(v1.iterValid(i1) && v2.iterValid(i2)) {
      final int d1 = v1.iterDim(i1), d2 = v2.iterDim(i2);
      if(d1 < d2) {
        final double val = v1.iterDoubleValue(i1);
        l1 += val * val;
        i1 = v1.iterAdvance(i1);
      }
      else if(d2 < d1) {
        final double val = v2.iterDoubleValue(i2);
        l2 += val * val;
        i2 = v2.iterAdvance(i2);
      }
      else { // d1 == d2
        final double val1 = v1.iterDoubleValue(i1);
        final double val2 = v2.iterDoubleValue(i2);
        l1 += val1 * val1;
        l2 += val2 * val2;
        cross += val1 * val2;
        i1 = v1.iterAdvance(i1);
        i2 = v2.iterAdvance(i2);
      }
    }
    while(v1.iterValid(i1)) {
      final double val = v1.iterDoubleValue(i1);
      l1 += val * val;
      i1 = v1.iterAdvance(i1);
    }
    while(v2.iterValid(i2)) {
      final double val = v2.iterDoubleValue(i2);
      l2 += val * val;
      i2 = v2.iterAdvance(i2);
    }
    if(cross == 0.) {
      return 0.;
    }
    if(l1 == 0. || l2 == 0.) {
      return 1.;
    }
    final double a = Math.sqrt((cross / l1) * (cross / l2));
    return (a < 1.) ? a : 1.;
  }

  /**
   * Compute the angle between two vectors.
   *
   * @param v1 first vector
   * @param v2 second vector
   * @param o Origin
   * @return Angle
   */
  public static double angle(NumberVector v1, NumberVector v2, Vector o) {
    final int dim1 = v1.getDimensionality(), dim2 = v2.getDimensionality(),
        dimo = o.getDimensionality();
    final int mindim = (dim1 <= dim2) ? dim1 : dim2;
    // Essentially, we want to compute this:
    // v1' = v1 - o, v2' = v2 - o
    // v1'.transposeTimes(v2') / (v1'.euclideanLength()*v2'.euclideanLength());
    // We can just compute all three in parallel.
    double[] oe = o.getArrayRef();
    double cross = 0., l1 = 0., l2 = 0.;
    for(int k = 0; k < mindim; k++) {
      final double dk = k < dimo ? oe[k] : 0.;
      final double r1 = v1.doubleValue(k) - dk;
      final double r2 = v2.doubleValue(k) - dk;
      cross += r1 * r2;
      l1 += r1 * r1;
      l2 += r2 * r2;
    }
    for(int k = mindim; k < dim1; k++) {
      final double dk = k < dimo ? oe[k] : 0.;
      final double r1 = v1.doubleValue(k) - dk;
      l1 += r1 * r1;
    }
    for(int k = mindim; k < dim2; k++) {
      final double dk = k < dimo ? oe[k] : 0.;
      final double r2 = v2.doubleValue(k) - dk;
      l2 += r2 * r2;
    }
    if(cross == 0.) {
      return 0.;
    }
    if(l1 == 0. || l2 == 0.) {
      return 1.;
    }
    final double a = Math.sqrt((cross / l1) * (cross / l2));
    return (a < 1.) ? a : 1.;
  }

  /**
   * Compute the angle between two vectors.
   *
   * @param v1 first vector
   * @param v2 second vector
   * @param o Origin
   * @return Angle
   */
  public static double angle(NumberVector v1, NumberVector v2, NumberVector o) {
    final int dim1 = v1.getDimensionality(), dim2 = v2.getDimensionality(),
        dimo = o.getDimensionality();
    final int mindim = (dim1 <= dim2) ? dim1 : dim2;
    // Essentially, we want to compute this:
    // v1' = v1 - o, v2' = v2 - o
    // v1'.transposeTimes(v2') / (v1'.euclideanLength()*v2'.euclideanLength());
    // We can just compute all three in parallel.
    double cross = 0, l1 = 0, l2 = 0;
    for(int k = 0; k < mindim; k++) {
      final double ok = k < dimo ? o.doubleValue(k) : 0.;
      final double r1 = v1.doubleValue(k) - ok;
      final double r2 = v2.doubleValue(k) - o.doubleValue(k);
      cross += r1 * r2;
      l1 += r1 * r1;
      l2 += r2 * r2;
    }
    for(int k = mindim; k < dim1; k++) {
      final double ok = k < dimo ? o.doubleValue(k) : 0.;
      final double r1 = v1.doubleValue(k) - ok;
      l1 += r1 * r1;
    }
    for(int k = mindim; k < dim2; k++) {
      final double ok = k < dimo ? o.doubleValue(k) : 0.;
      final double r2 = v2.doubleValue(k) - ok;
      l2 += r2 * r2;
    }
    if(cross == 0.) {
      return 0.;
    }
    if(l1 == 0. || l2 == 0.) {
      return 1.;
    }
    final double a = Math.sqrt((cross / l1) * (cross / l2));
    return (a < 1.) ? a : 1.;
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
  public static double cosAngle(NumberVector v1, NumberVector v2) {
    if(v1 instanceof SparseNumberVector && v2 instanceof SparseNumberVector) {
      return angleSparse((SparseNumberVector) v1, (SparseNumberVector) v2);
    }
    final int dim1 = v1.getDimensionality(), dim2 = v2.getDimensionality();
    final int mindim = (dim1 <= dim2) ? dim1 : dim2;
    // Essentially, we want to compute this:
    // v1.transposeTimes(v2) / (v1.euclideanLength() * v2.euclideanLength());
    // We can just compute all three in parallel.
    double cross = 0, l1 = 0, l2 = 0;
    for(int k = 0; k < mindim; k++) {
      final double r1 = v1.doubleValue(k);
      final double r2 = v2.doubleValue(k);
      cross += r1 * r2;
      l1 += r1 * r1;
      l2 += r2 * r2;
    }
    for(int k = mindim; k < dim1; k++) {
      final double r1 = v1.doubleValue(k);
      l1 += r1 * r1;
    }
    for(int k = mindim; k < dim2; k++) {
      final double r2 = v2.doubleValue(k);
      l2 += r2 * r2;
    }
    if(cross == 0.) {
      return 0.;
    }
    if(l1 == 0. || l2 == 0.) {
      return 1.;
    }
    final double a = Math.sqrt((cross / l1) * (cross / l2));
    return (a < 1.) ? a : 1.;
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
    if(v1 instanceof NumberVector && v2 instanceof NumberVector) {
      return cosAngle((NumberVector) v1, (NumberVector) v2);
    }
    final int dim1 = v1.getDimensionality(), dim2 = v2.getDimensionality();
    final int mindim = (dim1 <= dim2) ? dim1 : dim2;
    // Essentially, we want to compute this:
    // absmax(v1.transposeTimes(v2))/(min(v1.euclideanLength())*min(v2.euclideanLength()));
    // We can just compute all three in parallel.
    double s1 = 0, s2 = 0, l1 = 0, l2 = 0;
    for(int k = 0; k < mindim; k++) {
      final double min1 = v1.getMin(k), max1 = v1.getMax(k);
      final double min2 = v2.getMin(k), max2 = v2.getMax(k);
      final double p1 = min1 * min2, p2 = min1 * max2;
      final double p3 = max1 * min2, p4 = max1 * max2;
      s1 += MathUtil.max(p1, p2, p3, p4);
      s2 += MathUtil.min(p1, p2, p3, p4);
      if(max1 < 0) {
        l1 += max1 * max1;
      }
      else if(min1 > 0) {
        l1 += min1 * min1;
      } // else: 0
      if(max2 < 0) {
        l2 += max2 * max2;
      }
      else if(min2 > 0) {
        l2 += min2 * min2;
      } // else: 0
    }
    for(int k = mindim; k < dim1; k++) {
      final double min1 = v1.getMin(k), max1 = v1.getMax(k);
      if(max1 < 0.) {
        l1 += max1 * max1;
      }
      else if(min1 > 0.) {
        l1 += min1 * min1;
      } // else: 0
    }
    for(int k = mindim; k < dim2; k++) {
      final double min2 = v2.getMin(k), max2 = v2.getMax(k);
      if(max2 < 0.) {
        l2 += max2 * max2;
      }
      else if(min2 > 0.) {
        l2 += min2 * min2;
      } // else: 0
    }
    final double cross = MathUtil.max(s1, Math.abs(s2));
    if(cross == 0.) {
      return 0.;
    }
    if(l1 == 0. || l2 == 0.) {
      return 1.;
    }
    final double a = Math.sqrt((cross / l1) * (cross / l2));
    return (a < 1.) ? a : 1.;
  }

  /**
   * Compute the scalar product (inner product) of this and the given
   * DoubleVector.
   *
   * @param d1 the first vector to compute the scalar product for
   * @param d2 the second vector to compute the scalar product for
   * @return the scalar product (inner product) of this and the given
   *         DoubleVector
   */
  public static double scalarProduct(NumberVector d1, NumberVector d2) {
    final int dim = d1.getDimensionality();
    double result = 0.;
    for(int i = 0; i < dim; i++) {
      result += d1.doubleValue(i) * d2.doubleValue(i);
    }
    return result;
  }

  /**
   * Compute medoid for a given subset.
   *
   * @param relation Relation to process
   * @param sample Sample set
   * @return Medoid vector
   */
  public static Vector computeMedoid(Relation<? extends NumberVector> relation, DBIDs sample) {
    final int dim = RelationUtil.dimensionality(relation);
    ArrayModifiableDBIDs mids = DBIDUtil.newArray(sample);
    SortDBIDsBySingleDimension s = new SortDBIDsBySingleDimension(relation);
    Vector medoid = new Vector(dim);
    DBIDArrayIter it = mids.iter();
    for(int d = 0; d < dim; d++) {
      s.setDimension(d);
      it.seek(QuickSelect.median(mids, s));
      medoid.set(d, relation.get(it).doubleValue(d));
    }
    return medoid;
  }

  /**
   * This is an ugly hack, but we don't want to have the {@link Matrix} class
   * depend on {@link NumberVector}. Maybe a future version will no longer need
   * this.
   *
   * @param mat Matrix
   * @param v Vector
   * @return {@code mat * v}, as double array.
   */
  public static double[] fastTimes(Matrix mat, NumberVector v) {
    final double[][] elements = mat.getArrayRef();
    final int cdim = mat.getColumnDimensionality();
    final double[] X = new double[elements.length];
    // multiply it with each row from A
    for(int i = 0; i < elements.length; i++) {
      final double[] Arowi = elements[i];
      double s = 0;
      for(int k = 0; k < cdim; k++) {
        s += Arowi[k] * v.doubleValue(k);
      }
      X[i] = s;
    }
    return X;
  }

  /**
   * Compare number vectors by a single dimension.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class SortDBIDsBySingleDimension implements Comparator<DBIDRef> {
    /**
     * Dimension to sort with.
     */
    private int d;

    /**
     * The relation to sort.
     */
    private Relation<? extends NumberVector> data;

    /**
     * Constructor.
     *
     * @param data Vector data source
     * @param dim Dimension to sort by
     */
    public SortDBIDsBySingleDimension(Relation<? extends NumberVector> data, int dim) {
      super();
      this.data = data;
      this.d = dim;
    };

    /**
     * Constructor.
     *
     * @param data Vector data source
     */
    public SortDBIDsBySingleDimension(Relation<? extends NumberVector> data) {
      super();
      this.data = data;
    };

    /**
     * Get the dimension to sort by.
     *
     * @return Dimension to sort with
     */
    public int getDimension() {
      return this.d;
    }

    /**
     * Set the dimension to sort by.
     *
     * @param d2 Dimension to sort with
     */
    public void setDimension(int d2) {
      this.d = d2;
    }

    @Override
    public int compare(DBIDRef id1, DBIDRef id2) {
      final double v1 = data.get(id1).doubleValue(d);
      final double v2 = data.get(id2).doubleValue(d);
      return v1 < v2 ? -1 : v1 > v2 ? +1 : 0;
    }
  }

  /**
   * Compare number vectors by a single dimension.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class SortVectorsBySingleDimension implements Comparator<NumberVector> {
    /**
     * Dimension to sort with.
     */
    private int d;

    /**
     * Constructor.
     *
     * @param dim Dimension to sort by.
     */
    public SortVectorsBySingleDimension(int dim) {
      super();
      this.d = dim;
    };

    /**
     * Constructor.
     */
    public SortVectorsBySingleDimension() {
      super();
    };

    /**
     * Get the dimension to sort by.
     *
     * @return Dimension to sort with
     */
    public int getDimension() {
      return this.d;
    }

    /**
     * Set the dimension to sort by.
     *
     * @param d2 Dimension to sort with
     */
    public void setDimension(int d2) {
      this.d = d2;
    }

    @Override
    public int compare(NumberVector o1, NumberVector o2) {
      final double v1 = o1.doubleValue(d);
      final double v2 = o2.doubleValue(d);
      return v1 < v2 ? -1 : v1 > v2 ? +1 : 0;
    }
  }

  /**
   * Project a number vector to the specified attributes.
   *
   * @param v a NumberVector to project
   * @param selectedAttributes the attributes selected for projection
   * @param factory Vector factory
   * @param <V> Vector type
   * @return a new NumberVector as a projection on the specified attributes
   */
  public static <V extends NumberVector> V project(V v, long[] selectedAttributes, NumberVector.Factory<V> factory) {
    int card = BitsUtil.cardinality(selectedAttributes);
    if(factory instanceof SparseNumberVector.Factory) {
      final SparseNumberVector.Factory<?> sfactory = (SparseNumberVector.Factory<?>) factory;
      TIntDoubleHashMap values = new TIntDoubleHashMap(card, 1);
      for(int d = BitsUtil.nextSetBit(selectedAttributes, 0); d >= 0; d = BitsUtil.nextSetBit(selectedAttributes, d + 1)) {
        if(v.doubleValue(d) != 0.0) {
          values.put(d, v.doubleValue(d));
        }
      }
      // We can't avoid this cast, because Java doesn't know that V is a
      // SparseNumberVector:
      @SuppressWarnings("unchecked")
      V projectedVector = (V) sfactory.newNumberVector(values, card);
      return projectedVector;
    }
    else {
      double[] newAttributes = new double[card];
      int i = 0;
      for(int d = BitsUtil.nextSetBit(selectedAttributes, 0); d >= 0; d = BitsUtil.nextSetBit(selectedAttributes, d + 1)) {
        newAttributes[i] = v.doubleValue(d);
        i++;
      }
      return factory.newNumberVector(newAttributes);
    }
  }
}
