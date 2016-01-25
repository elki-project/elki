package de.lmu.ifi.dbs.elki.utilities;

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

import java.util.Comparator;
import java.util.Random;

/**
 * This class collects various static helper methods.
 * 
 * For helper methods related to special application fields see other utilities
 * classes.
 * 
 * @see de.lmu.ifi.dbs.elki.utilities
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.2
 */
public final class Util {
  /**
   * Prime number used in hash code computation.
   */
  private static final long HASHPRIME = 2654435761L;

  /**
   * Detect Java 7.
   */
  public static final boolean IS_JAVA7 = System.getProperty("java.version").startsWith("1.7.");

  /**
   * Detect Oracle Java.
   */
  public static final boolean IS_ORACLE_JAVA = System.getProperty("java.vm.vendor").startsWith("Oracle");

  /**
   * Fake constructor: do not instantiate.
   */
  private Util() {
    // Do not instantiate.
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
  public static long[] randomBitSet(int cardinality, int capacity, Random random) {
    assert (cardinality >= 0) : "Cannot set a negative number of bits!";
    assert (cardinality < capacity) : "Cannot set " + cardinality + " of " + capacity + " bits!";
    // FIXME: Avoid recomputing the cardinality.
    if(cardinality < capacity >>> 1) {
      long[] bitset = BitsUtil.zero(capacity);
      while(BitsUtil.cardinality(bitset) < cardinality) {
        BitsUtil.setI(bitset, random.nextInt(capacity));
      }
      return bitset;
    }
    else {
      long[] bitset = BitsUtil.ones(capacity);
      while(BitsUtil.cardinality(bitset) > cardinality) {
        BitsUtil.clearI(bitset, random.nextInt(capacity));
      }
      return bitset;
    }
  }

  /**
   * Mix multiple hashcodes into one.
   * 
   * @param hash Single Hashcodes to "mix"
   * @return Original hash code
   */
  @Deprecated
  public static int mixHashCodes(int hash) {
    return hash;
  }

  /**
   * Mix multiple hashcodes into one.
   * 
   * @param hash1 First hashcode to mix
   * @param hash2 Second hashcode to mix
   * @return Mixed hash code
   */
  public static int mixHashCodes(int hash1, int hash2) {
    return (int) (hash1 * HASHPRIME + hash2);
  }

  /**
   * Mix multiple hashcodes into one.
   * 
   * @param hash1 First hashcode to mix
   * @param hash2 Second hashcode to mix
   * @param hash3 Third hashcode to mix
   * @return Mixed hash code
   */
  public static int mixHashCodes(int hash1, int hash2, int hash3) {
    long result = hash1 * HASHPRIME + hash2;
    return (int) (result * HASHPRIME + hash3);
  }

  /**
   * Mix multiple hashcodes into one.
   * 
   * @param hash Hashcodes to mix
   * @return Mixed hash code
   */
  public static int mixHashCodes(int... hash) {
    if(hash.length == 0) {
      return 0;
    }
    long result = hash[0];
    for(int i = 1; i < hash.length; i++) {
      result = result * HASHPRIME + hash[i];
    }
    return (int) result;
  }

  /**
   * Static instance.
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
   * @param <T> Object type
   * @return Forward comparator
   */
  @SuppressWarnings("unchecked")
  public static <T> Comparator<T> forwardOrder() {
    return (Comparator<T>) FORWARD;
  }
}
