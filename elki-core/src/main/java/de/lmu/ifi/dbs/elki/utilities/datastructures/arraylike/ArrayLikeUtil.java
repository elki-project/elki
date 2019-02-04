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
package de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;

/**
 * Utility class that allows plug-in use of various "array-like" types such as
 * lists in APIs that can take any kind of array to safe the cost of
 * reorganizing the objects into a real array.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @opt nodefillcolor LemonChiffon
 * @composed - - - ArrayAdapter
 */
public final class ArrayLikeUtil {
  /**
   * Static instance for lists.
   */
  private static final ListArrayAdapter<Object> LISTADAPTER = new ListArrayAdapter<>();

  /**
   * Static instance for lists of numbers.
   */
  private static final NumberListArrayAdapter<Number> NUMBERLISTADAPTER = new NumberListArrayAdapter<>();

  /**
   * Static instance.
   */
  public static final FeatureVectorAdapter<?> FEATUREVECTORADAPTER = FeatureVectorAdapter.STATIC;

  /**
   * Use a number vector in the array API.
   */
  public static final NumberVectorAdapter NUMBERVECTORADAPTER = NumberVectorAdapter.STATIC;

  /**
   * Use a double array in the array API.
   */
  public static final NumberArrayAdapter<Double, double[]> DOUBLEARRAYADAPTER = DoubleArrayAdapter.STATIC;

  /**
   * Use a float array in the array API.
   */
  public static final NumberArrayAdapter<Float, float[]> FLOATARRAYADAPTER = FloatArrayAdapter.STATIC;

  /**
   * Fake constructor. Do not instantiate!
   */
  private ArrayLikeUtil() {
    // Do not instantiate
  }

  /**
   * Cast the static instance.
   *
   * @param dummy Dummy variable, for type inference
   * @return Static instance
   */
  @SuppressWarnings("unchecked")
  public static <T> ArrayAdapter<T, List<? extends T>> listAdapter(List<? extends T> dummy) {
    return (ListArrayAdapter<T>) LISTADAPTER;
  }

  /**
   * Cast the static instance.
   *
   * @param dummy Dummy variable, for type inference
   * @return Static instance
   */
  @SuppressWarnings("unchecked")
  public static <T extends Number> NumberArrayAdapter<T, List<? extends T>> numberListAdapter(List<? extends T> dummy) {
    return (NumberListArrayAdapter<T>) NUMBERLISTADAPTER;
  }

  /**
   * Get the static instance.
   *
   * @param prototype Prototype value, for type inference
   * @return Instance
   */
  @SuppressWarnings("unchecked")
  public static <F> FeatureVectorAdapter<F> featureVectorAdapter(FeatureVector<F> prototype) {
    return (FeatureVectorAdapter<F>) FEATUREVECTORADAPTER;
  }

  /**
   * Returns the index of the maximum of the given values. If no value is bigger
   * than the first, the index of the first entry is returned.
   *
   * @param <A> array type
   * @param array Array to inspect
   * @param adapter API adapter class
   * @return the index of the maximum in the given values
   * @throws IndexOutOfBoundsException if the length of the array is 0.
   */
  public static <A> int getIndexOfMaximum(A array, NumberArrayAdapter<?, A> adapter) throws IndexOutOfBoundsException {
    final int size = adapter.size(array);
    int index = 0;
    double max = adapter.getDouble(array, 0);
    for (int i = 1; i < size; i++) {
      double val = adapter.getDouble(array, i);
      if (val > max) {
        max = val;
        index = i;
      }
    }
    return index;
  }

  /**
   * Returns the index of the maximum of the given values. If no value is bigger
   * than the first, the index of the first entry is returned.
   *
   * @param array Array to inspect
   * @return the index of the maximum in the given values
   * @throws IndexOutOfBoundsException if the length of the array is 0.
   */
  public static int getIndexOfMaximum(double[] array) throws IndexOutOfBoundsException {
    return getIndexOfMaximum(array, DOUBLEARRAYADAPTER);
  }

  /**
   * Convert a numeric array-like to a <code>double[]</code>.
   *
   * @param array Array-like
   * @param adapter Adapter
   * @return primitive double array
   */
  public static <A> double[] toPrimitiveDoubleArray(A array, NumberArrayAdapter<?, ? super A> adapter) {
    if (adapter == DOUBLEARRAYADAPTER) {
      return ((double[]) array).clone();
    }
    double[] ret = new double[adapter.size(array)];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = adapter.getDouble(array, i);
    }
    return ret;
  }

  /**
   * Convert a list of numbers to <code>double[]</code>.
   *
   * @param array List of numbers
   * @return double array
   */
  public static double[] toPrimitiveDoubleArray(List<? extends Number> array) {
    return toPrimitiveDoubleArray(array, NUMBERLISTADAPTER);
  }

  /**
   * Convert a number vector to <code>double[]</code>.
   *
   * @param obj Object to convert
   * @return primitive double array
   */
  public static double[] toPrimitiveDoubleArray(NumberVector obj) {
    return toPrimitiveDoubleArray(obj, NUMBERVECTORADAPTER);
  }

  /**
   * Convert a numeric array-like to a <code>float[]</code>.
   *
   * @param array Array-like
   * @param adapter Adapter
   * @return primitive float array
   */
  public static <A> float[] toPrimitiveFloatArray(A array, NumberArrayAdapter<?, ? super A> adapter) {
    if (adapter == FLOATARRAYADAPTER) {
      return ((float[]) array).clone();
    }
    float[] ret = new float[adapter.size(array)];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = adapter.getFloat(array, i);
    }
    return ret;
  }

  /**
   * Convert a list of numbers to <code>float[]</code>.
   *
   * @param array List of numbers
   * @return float array
   */
  public static float[] toPrimitiveFloatArray(List<? extends Number> array) {
    return toPrimitiveFloatArray(array, NUMBERLISTADAPTER);
  }

  /**
   * Convert a number vector to <code>float[]</code>.
   *
   * @param obj Object to convert
   * @return primitive float array
   */
  public static float[] toPrimitiveFloatArray(NumberVector obj) {
    return toPrimitiveFloatArray(obj, NUMBERVECTORADAPTER);
  }

  /**
   * Convert a numeric array-like to a <code>int[]</code>.
   *
   * @param array Array-like
   * @param adapter Adapter
   * @return primitive double array
   */
  public static <A> int[] toPrimitiveIntegerArray(A array, NumberArrayAdapter<?, ? super A> adapter) {
    int[] ret = new int[adapter.size(array)];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = adapter.getInteger(array, i);
    }
    return ret;
  }

  /**
   * Convert a list of numbers to <code>int[]</code>.
   *
   * @param array List of numbers
   * @return double array
   */
  public static int[] toPrimitiveIntegerArray(List<? extends Number> array) {
    return toPrimitiveIntegerArray(array, NUMBERLISTADAPTER);
  }

  /**
   * Convert a number vector to <code>int[]</code>.
   *
   * @param obj Object to convert
   * @return primitive double array
   */
  public static int[] toPrimitiveIntegerArray(NumberVector obj) {
    return toPrimitiveIntegerArray(obj, NUMBERVECTORADAPTER);
  }
}
