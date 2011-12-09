package de.lmu.ifi.dbs.elki.utilities;

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

import gnu.trove.map.hash.TIntFloatHashMap;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.SparseFloatVector;

/**
 * This class collects various static helper methods.
 * 
 * For helper methods related to special application fields see other utilities
 * classes.
 * 
 * 
 * @see de.lmu.ifi.dbs.elki.utilities
 */
public final class Util {
  /**
   * Returns a new double array containing the same objects as are contained in
   * the given array.
   * 
   * @param array an array to copy
   * @return the copied array
   */
  public static double[] copy(double[] array) {
    double[] copy = new double[array.length];
    System.arraycopy(array, 0, copy, 0, array.length);
    return copy;
  }

  /**
   * Returns the unboxed double array of the given Object Double array.
   * 
   * @param array the array to be unboxed
   * @return the unboxed double array
   */
  public static double[] unbox(Double[] array) {
    double[] unboxed = new double[array.length];
    // noinspection ManualArrayCopy
    for(int i = 0; i < unboxed.length; i++) {
      unboxed[i] = array[i];
    }
    return unboxed;
  }

  /**
   * Returns a new <code>Double</code> array initialized to the values
   * represented by the specified <code>String</code> and separated by comma, as
   * performed by the <code>valueOf</code> method of class <code>Double</code>.
   * 
   * @param s the string to be parsed.
   * @return a new <code>Double</code> array represented by s
   */
  public static double[] parseDoubles(String s) {
    List<Double> result = new ArrayList<Double>();
    StringTokenizer tokenizer = new StringTokenizer(s, ",");
    while(tokenizer.hasMoreTokens()) {
      String d = tokenizer.nextToken();
      result.add(Double.parseDouble(d));
    }
    return unbox(result.toArray(new Double[result.size()]));
  }

  /**
   * Converts the specified array of doubles to an array of floats.
   * 
   * @param values the array of doubles to be converted
   * @return the converted array of floats
   */
  public static double[] convertToDoubles(float[] values) {
    double[] result = new double[values.length];
    for(int i = 0; i < values.length; i++) {
      result[i] = values[i];
    }
    return result;
  }

  /**
   * Prints the given list to the specified PrintStream. The list entries are
   * separated by the specified separator. The last entry is not followed by a
   * separator. Thus, if a newline is used as separator, it might make sense to
   * print a newline to the PrintStream after calling this method.
   * 
   * @param <O> object class
   * @param list the list to be printed
   * @param separator the separator to separate entries of the list
   * @param out the target PrintStream
   */
  public static <O> void print(List<O> list, String separator, PrintStream out) {
    for(Iterator<O> iter = list.iterator(); iter.hasNext();) {
      out.print(iter.next());
      if(iter.hasNext()) {
        out.print(separator);
      }
    }
  }

  /**
   * Returns the index of the maximum of the given values. If no value is bigger
   * than the first, the index of the first entry is returned.
   * 
   * @param values the values to find the index of the maximum
   * @return the index of the maximum in the given values
   * @throws ArrayIndexOutOfBoundsException if <code>values.length==0</code>
   */
  public static int getIndexOfMaximum(double[] values) throws ArrayIndexOutOfBoundsException {
    int index = 0;
    double max = values[index];
    for(int i = 0; i < values.length; i++) {
      if(values[i] > max) {
        max = values[i];
        index = i;
      }
    }
    return index;
  }

  /**
   * Creates a new BitSet of fixed cardinality with randomly set bits.
   * 
   * @param cardinality the cardinality of the BitSet to create
   * @param capacity the capacity of the BitSet to create - the randomly
   *        generated indices of the bits set to true will be uniformly
   *        distributed between 0 (inclusive) and capacity (exclusive)
   * @param random a Random Object to create the sequence of indices set to true
   *        - the same number occurring twice or more is ignored but the already
   *        selected bit remains true
   * @return a new BitSet with randomly set bits
   */
  public static BitSet randomBitSet(int cardinality, int capacity, Random random) {
    BitSet bitset = new BitSet(capacity);
    while(bitset.cardinality() < cardinality) {
      bitset.set(random.nextInt(capacity));
    }
    return bitset;
  }

  /**
   * Provides a new DoubleVector as a projection on the specified attributes.
   * 
   * If the given DoubleVector has already an ID not <code>null</code>, the same
   * ID is set in the returned new DoubleVector. Nevertheless, the returned
   * DoubleVector is not backed by the given DoubleVector, i.e., any changes
   * affecting <code>v</code> after calling this method will not affect the
   * newly returned DoubleVector.
   * 
   * @param v a DoubleVector to project
   * @param selectedAttributes the attributes selected for projection
   * @return a new DoubleVector as a projection on the specified attributes
   * @throws IllegalArgumentException if the given selected attributes specify
   *         an attribute as selected which is out of range for the given
   *         DoubleVector.
   * @see DoubleVector#doubleValue(int)
   */
  public static DoubleVector project(DoubleVector v, BitSet selectedAttributes) {
    double[] newAttributes = new double[selectedAttributes.cardinality()];
    int i = 0;
    for(int d = selectedAttributes.nextSetBit(0); d >= 0; d = selectedAttributes.nextSetBit(d + 1)) {
      newAttributes[i] = v.doubleValue(d + 1);
      i++;
    }
    DoubleVector projectedVector = new DoubleVector(newAttributes);
    return projectedVector;
  }

  /**
   * Provides a new SparseFloatVector as a projection on the specified
   * attributes.
   * 
   * If the given SparseFloatVector has already an ID not <code>null</code>, the
   * same ID is set in the returned new SparseFloatVector. Nevertheless, the
   * returned SparseFloatVector is not backed by the given SparseFloatVector,
   * i.e., any changes affecting <code>v</code> after calling this method will
   * not affect the newly returned SparseFloatVector.
   * 
   * @param v a SparseFloatVector to project
   * @param selectedAttributes the attributes selected for projection
   * @return a new SparseFloatVector as a projection on the specified attributes
   * @throws IllegalArgumentException if the given selected attributes specify
   *         an attribute as selected which is out of range for the given
   *         SparseFloatVector.
   */
  public static SparseFloatVector project(SparseFloatVector v, BitSet selectedAttributes) {
    TIntFloatHashMap values = new TIntFloatHashMap(selectedAttributes.cardinality(), 1);
    for(int d = selectedAttributes.nextSetBit(0); d >= 0; d = selectedAttributes.nextSetBit(d + 1)) {
      if(v.getValue(d + 1) != 0.0f) {
        values.put(d, v.getValue(d + 1));
      }
    }
    SparseFloatVector projectedVector = new SparseFloatVector(values, selectedAttributes.cardinality());
    return projectedVector;
  }

  /**
   * Search an (unsorted) array linearly for an object.
   * 
   * @param arr Array to search
   * @param ref Object to search for
   * @return Index of object or -1 if not found.
   */
  public static int arrayFind(String[] arr, Object ref) {
    for(int index = 0; index < arr.length; index++) {
      if(ref.equals(arr[index])) {
        return index;
      }
    }
    return -1;
  }

  /**
   * Mix multiple hashcodes into one.
   * 
   * @param hash Hashcodes to mix
   * @return Mixed hash code
   */
  public static final int mixHashCodes(int... hash) {
    final long prime = 2654435761L;
    if(hash.length == 0) {
      return 0;
    }
    long result = hash[0];
    for(int i = 1; i < hash.length; i++) {
      result = result * prime + hash[i];
    }
    return (int) result;
  }

  /**
   * Static instance
   */
  private static final Comparator<?> FORWARD = new ForwardComparator();

  /**
   * Regular comparator. See {@link java.util.Collections#reverseOrder()} for a
   * reverse comparator.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private static final class ForwardComparator implements Comparator<Comparable<Object>> {
    @Override
    public int compare(Comparable<Object> o1, Comparable<Object> o2) {
      return o1.compareTo(o2);
    }
  }

  /**
   * Compare two objects, forward. See
   * {@link java.util.Collections#reverseOrder()} for a reverse comparator.
   * 
   * @return Forward comparator
   */
  @SuppressWarnings("unchecked")
  public static final <T> Comparator<T> forwardOrder() {
    return (Comparator<T>) FORWARD;
  }
}