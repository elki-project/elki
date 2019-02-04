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

/**
 * Sublist of an existing result to contain only some of the elements.
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
public class DoubleIntegerDBIDSubList implements DoubleIntegerDBIDList {
  /**
   * Start offset.
   */
  private final int begin;

  /**
   * End offset.
   */
  private final int end;

  /**
   * Wrapped inner result.
   */
  private final DoubleIntegerDBIDList inner;

  /**
   * Constructor.
   *
   * @param inner Inner instance
   * @param begin Begin offset
   * @param end End offset
   */
  public DoubleIntegerDBIDSubList(DoubleIntegerDBIDList inner, int begin, int end) {
    this.inner = inner;
    assert (end < inner.size()) : "Access beyond size of list.";
    assert (begin >= 0 && end >= begin);
    this.begin = begin;
    this.end = end;
  }

  @Override
  public DBIDVar assignVar(int index, DBIDVar var) {
    index += begin;
    assert index < end : "Access beyond size of list.";
    return inner.assignVar(index, var);
  }

  @Override
  public double doubleValue(int index) {
    assert index < end : "Access beyond size of list.";
    return inner.doubleValue(index);
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
  public boolean isEmpty() {
    return begin == end;
  }

  @Override
  public int size() {
    return end - begin;
  }

  @Override
  public DoubleIntegerDBIDList slice(int begin, int end) {
    begin += this.begin;
    end += this.begin;
    assert (end < this.end) : "Access beyond size of list.";
    return new DoubleIntegerDBIDSubList(inner, begin, end);
  }

  /**
   * Iterator for the sublist.
   *
   * @author Erich Schubert
   */
  private class Itr implements DoubleIntegerDBIDListIter {
    /**
     * Current position.
     */
    private DoubleIntegerDBIDListIter it = inner.iter().seek(begin);

    @Override
    public boolean valid() {
      return it.getOffset() < end && it.getOffset() >= begin;
    }

    @Override
    public Itr advance() {
      it.advance();
      return this;
    }

    @Override
    public double doubleValue() {
      return it.doubleValue();
    }

    @Override
    public int internalGetIndex() {
      return it.internalGetIndex();
    }

    @Override
    public int getOffset() {
      return it.getOffset() - begin;
    }

    @Override
    public Itr advance(int count) {
      it.advance(count);
      return this;
    }

    @Override
    public Itr retract() {
      it.retract();
      return this;
    }

    @Override
    public Itr seek(int off) {
      it.seek(begin + off);
      return this;
    }
  }
}
