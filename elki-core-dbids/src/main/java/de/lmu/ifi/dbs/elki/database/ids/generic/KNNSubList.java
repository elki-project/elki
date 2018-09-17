/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
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
package de.lmu.ifi.dbs.elki.database.ids.generic;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;

/**
 * Sublist of an existing result to contain only the first k elements.
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
public class KNNSubList implements KNNList {
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
  private final KNNList inner;

  /**
   * Constructor.
   *
   * @param inner Inner instance
   * @param k k value
   */
  public KNNSubList(KNNList inner, int k) {
    this.inner = inner;
    this.k = k;
    // Compute list size
    if(k < inner.getK()) {
      DoubleDBIDListIter iter = inner.iter();
      final double dist = iter.seek(k).doubleValue();
      while(iter.advance().valid()) {
        if(dist < iter.doubleValue()) {
          break;
        }
      }
      size = iter.getOffset() - 1;
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
    return inner.doubleValue(index);
  }

  @Override
  public double getKNNDistance() {
    return inner.doubleValue(k);
  }

  @Override
  public DoubleDBIDListIter iter() {
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
    return k < this.k ? new KNNSubList(inner, k) : this;
  }

  @Override
  public DoubleDBIDList slice(int begin, int end) {
    return inner.slice(begin, Math.min(size, end));
  }

  /**
   * Iterator for the sublist.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  private class Itr implements DoubleDBIDListIter {
    /**
     * Inner iterator
     */
    private DoubleDBIDListIter inneriter = inner.iter();

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
