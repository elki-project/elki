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
package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;

/**
 * Abstract base class for dependence measures.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public abstract class AbstractDependenceMeasure implements DependenceMeasure {
  /**
   * Clamp values to a given minimum and maximum.
   * 
   * @param value True value
   * @param min Minimum
   * @param max Maximum
   * @return {@code value}, unless smaller than {@code min} or larger than
   *         {@code max}.
   */
  protected static double clamp(double value, double min, double max) {
    return value < min ? min : value > max ? max : value;
  }

  /**
   * Compute ranks of all objects, normalized to [0;1]
   * (where 0 is the smallest value, 1 is the largest).
   * 
   * @param adapter Data adapter
   * @param data Data array
   * @param len Length of data
   * @return Array of scores
   */
  protected static <A> double[] computeNormalizedRanks(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
    // Sort the objects:
    int[] s1 = sortedIndex(adapter, data, len);
    final double norm = .5 / (len - 1);
    double[] ret = new double[len];
    for(int i = 0; i < len;) {
      final int start = i++;
      final double val = adapter.getDouble(data, s1[start]);
      while(i < len && adapter.getDouble(data, s1[i]) <= val) {
        i++;
      }
      final double score = (start + i - 1) * norm;
      for(int j = start; j < i; j++) {
        ret[s1[j]] = score;
      }
    }
    return ret;
  }

  /**
   * Compute ranks of all objects, ranging from 1 to len.
   *
   * Ties are given the average rank.
   *
   * @param adapter Data adapter
   * @param data Data array
   * @param len Length of data
   * @return Array of scores
   */
  protected static <A> double[] ranks(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
    return ranks(adapter, data, sortedIndex(adapter, data, len));
  }

  /**
   * Compute ranks of all objects, ranging from 1 to len.
   *
   * Ties are given the average rank.
   *
   * @param adapter Data adapter
   * @param data Data array
   * @param idx Data index
   * @return Array of scores
   */
  protected static <A> double[] ranks(final NumberArrayAdapter<?, A> adapter, final A data, int[] idx) {
    final int len = idx.length;
    double[] ret = new double[len];
    for(int i = 0; i < len;) {
      final int start = i++;
      final double val = adapter.getDouble(data, idx[start]);
      // Include ties:
      while(i < len && adapter.getDouble(data, idx[i]) <= val) {
        i++;
      }
      final double score = (start + i - 1) * .5 + 1;
      for(int j = start; j < i; j++) {
        ret[idx[j]] = score;
      }
    }
    return ret;
  }

  /**
   * Build a sorted index of objects.
   *
   * @param adapter Data adapter
   * @param data Data array
   * @param len Length of data
   * @return Sorted index
   */
  protected static <A> int[] sortedIndex(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
    int[] s1 = MathUtil.sequence(0, len);
    IntegerArrayQuickSort.sort(s1, (x, y) -> Double.compare(adapter.getDouble(data, x), adapter.getDouble(data, y)));
    return s1;
  }

  /**
   * Discretize a data set into equi-width bin numbers.
   * 
   * @param adapter Data adapter
   * @param data Data array
   * @param len Length of data
   * @param bins Number of bins
   * @return Array of bin numbers [0;bin[
   */
  protected static <A> int[] discretize(NumberArrayAdapter<?, A> adapter, A data, final int len, final int bins) {
    double min = adapter.getDouble(data, 0), max = min;
    for(int i = 1; i < len; i++) {
      double v = adapter.getDouble(data, i);
      if(v < min) {
        min = v;
      }
      else if(v > max) {
        max = v;
      }
    }
    final double scale = (max > min) ? bins / (max - min) : 1;
    int[] discData = new int[len];
    for(int i = 0; i < len; i++) {
      int bin = (int) Math.floor((adapter.getDouble(data, i) - min) * scale);
      discData[i] = bin < 0 ? 0 : bin >= bins ? bins - 1 : bin;
    }
    return discData;
  }

  /**
   * Index into the serialized array.
   * 
   * @param x Column
   * @param y Row
   * @return Index in serialized array
   */
  protected static int index(int x, int y) {
    assert (x < y) : "Only lower triangle is allowed.";
    return ((y * (y - 1)) >> 1) + x;
  }

  /**
   * Validate the length of the two data sets (must be the same, and non-zero)
   * 
   * @param adapter1 First data adapter
   * @param data1 First data set
   * @param adapter2 Second data adapter
   * @param data2 Second data set
   * @param <A> First array type
   * @param <B> Second array type
   */
  protected static <A, B> int size(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2) {
    final int len = adapter1.size(data1);
    if(len != adapter2.size(data2)) {
      throw new IllegalArgumentException("Array sizes do not match!");
    }
    if(len == 0) {
      throw new IllegalArgumentException("Empty array!");
    }
    return len;
  }

  /**
   * Validate the length of the two data sets (must be the same, and non-zero)
   * 
   * @param adapter Data adapter
   * @param data Data sets
   * @param <A> First array type
   */
  protected static <A> int size(NumberArrayAdapter<?, A> adapter, Collection<? extends A> data) {
    if(data.size() < 2) {
      throw new IllegalArgumentException("Need at least two axes to compute dependence measures.");
    }
    Iterator<? extends A> iter = data.iterator();
    final int len = adapter.size(iter.next());
    while(iter.hasNext()) {
      if(len != adapter.size(iter.next())) {
        throw new IllegalArgumentException("Array sizes do not match!");
      }
    }
    if(len == 0) {
      throw new IllegalArgumentException("Empty array!");
    }
    return len;
  }
}
