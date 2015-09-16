package ch.ethz.globis.pht;

/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011-2015
Eidgenössische Technische Hochschule Zürich (ETH Zurich)
Institute for Information Systems
GlobIS Group

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