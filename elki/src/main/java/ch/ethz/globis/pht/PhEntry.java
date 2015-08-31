/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht;

import java.util.Arrays;
import java.util.Comparator;

public class PhEntry<T> {
  private long[] key;
  private T value;
  public PhEntry(long[] key, T value) {
    this.key = key;
    this.value = value;
  }

  public long[] getKey() {
    return key;
  }

  public T getValue() {
    return value;
  }

  protected void set(long[] key, T value) {
    this.key = key;
    this.value = value;
  }

  public static final class KeyComparator<T> implements Comparator<PhEntry<T>> {

    private final long[] val;

    public KeyComparator(long[] val) {
      this.val = val;
    }

    @Override
    public int compare(PhEntry<T> o1, PhEntry<T> o2) {
      double d = distSQ(o1.key) - distSQ(o2.key);
      //Do not return d directly because it may exceed the limits or precision of Integer.
      return d > 0 ? 1 : (d < 0 ? -1 : 0);
    }

    private final double distSQ(long[] v) {
      double d = 0;
      for (int i = 0; i < val.length; i++) {
        double dl = v[i] - val[i];
        d += dl*dl;
      }
      return d;
    }

  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PhEntry)) return false;

    PhEntry<T> pvEntry = (PhEntry<T>) o;

    if (!Arrays.equals(key, pvEntry.key)) return false;
    if (value != null ? !value.equals(pvEntry.value) : pvEntry.value != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = key != null ? Arrays.hashCode(key) : 0;
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  public void setValue(T value) {
    this.value = value;
  }
}