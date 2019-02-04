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
package de.lmu.ifi.dbs.elki.database.ids.integer;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;

/**
 * Sublist of an existing result to contain only the first k elements.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class IntegerDBIDKNNSubList implements IntegerDBIDKNNList {
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
  private final IntegerDBIDKNNList inner;

  /**
   * Constructor.
   *
   * @param inner Inner instance
   * @param k k value
   */
  public IntegerDBIDKNNSubList(IntegerDBIDKNNList inner, int k) {
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
    return inner.doubleValue(k - 1);
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
