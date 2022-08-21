/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.database.ids.integer;

import java.util.function.DoubleUnaryOperator;

import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.database.ids.KNNList;

/**
 * Sublist of an existing result to contain only the first k elements.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class IntegerDBIDKNNSubList implements KNNList, DoubleIntegerDBIDList {
  /**
   * Parameter k.
   */
  private final int k;

  /**
   * Actual size, including ties.
   */
  private final int size;

  /**
   * Wrapped inner result.
   */
  private final DoubleIntegerDBIDKNNList inner;

  /**
   * Constructor.
   *
   * @param inner Inner instance
   * @param k k value
   */
  public IntegerDBIDKNNSubList(DoubleIntegerDBIDKNNList inner, int k) {
    this.inner = inner;
    this.k = k;
    // Compute list size
    if(k < inner.getK()) {
      DoubleIntegerDBIDListIter iter = inner.iter();
      final double kdist = iter.seek(k - 1).doubleValue();
      // Add all values tied:
      int i = k;
      for(iter.advance(); iter.valid() && iter.doubleValue() <= kdist; iter.advance()) {
        i++;
      }
      size = i;
    }
    else {
      size = inner.size();
    }
  }

  @Override
  public int getK() {
    return k;
  }

  @Override
  public DBIDVar assignVar(int index, DBIDVar var) {
    assert (index < size) : "Access beyond design size of list.";
    return inner.assignVar(index, var);
  }

  @Override
  public double doubleValue(int index) {
    assert (index < size) : "Access beyond design size of list.";
    return inner.doubleValue(index);
  }

  @Override
  public double getKNNDistance() {
    return k <= size ? inner.doubleValue(k - 1) : Double.POSITIVE_INFINITY;
  }

  @Override
  public Itr iter() {
    return new Itr();
  }

  @Override
  public boolean contains(DBIDRef o) {
    for(DBIDIter iter = iter(); iter.valid(); iter.advance()) {
      if(DBIDUtil.equal(iter, o)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public KNNList subList(int k) {
    return new IntegerDBIDKNNSubList(inner, k);
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(size() * 20 + 20).append("kNNSubList[");
    Itr iter = this.iter();
    if(iter.valid()) {
      buf.append(iter.doubleValue()).append(':').append(iter.internalGetIndex());
    }
    while(iter.advance().valid()) {
      buf.append(',').append(iter.doubleValue()).append(':').append(iter.internalGetIndex());
    }
    return buf.append(']').toString();
  }

  @Override
  public KNNList map(DoubleUnaryOperator f) {
    DoubleIntegerDBIDKNNList n = new DoubleIntegerDBIDKNNList(k, size);
    System.arraycopy(inner.ids, 0, n.ids, 0, size);
    for(int i = 0; i < size; i++) {
      n.dists[i] = f.applyAsDouble(inner.dists[i]);
    }
    n.size = size;
    return n;
  }

  /**
   * Iterator for the sublist.
   *
   * @author Erich Schubert
   */
  private class Itr implements DoubleIntegerDBIDListIter {
    /**
     * Inner iterator.
     */
    private DoubleIntegerDBIDListIter inneriter = inner.iter();

    @Override
    public boolean valid() {
      return inneriter.getOffset() < size && inneriter.valid();
    }

    @Override
    public Itr advance() {
      inneriter.advance();
      return this;
    }

    @Override
    public double doubleValue() {
      return inneriter.doubleValue();
    }

    @Override
    public int internalGetIndex() {
      return inneriter.internalGetIndex();
    }

    @Override
    public int getOffset() {
      return inneriter.getOffset();
    }

    @Override
    public Itr advance(int count) {
      inneriter.advance(count);
      return this;
    }

    @Override
    public Itr retract() {
      inneriter.retract();
      return this;
    }

    @Override
    public Itr seek(int off) {
      inneriter.seek(off);
      return this;
    }
  }
}
