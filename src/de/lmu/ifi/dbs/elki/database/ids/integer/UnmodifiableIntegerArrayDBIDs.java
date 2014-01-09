package de.lmu.ifi.dbs.elki.database.ids.integer;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;

/**
 * Unmodifiable wrapper for DBIDs.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses IntegerArrayDBIDs
 * @apiviz.has UnmodifiableDBIDIter
 */
public class UnmodifiableIntegerArrayDBIDs implements IntegerArrayStaticDBIDs {
  /**
   * The DBIDs we wrap.
   */
  private final IntegerArrayDBIDs inner;

  /**
   * Constructor.
   * 
   * @param inner Inner DBID collection.
   */
  public UnmodifiableIntegerArrayDBIDs(IntegerArrayDBIDs inner) {
    super();
    this.inner = inner;
  }

  @Override
  public boolean contains(DBIDRef o) {
    return inner.contains(o);
  }

  @Override
  public boolean isEmpty() {
    return inner.isEmpty();
  }

  @Override
  public IntegerDBIDArrayIter iter() {
    IntegerDBIDArrayIter it = inner.iter();
    if (it instanceof DBIDMIter) {
      return new UnmodifiableDBIDIter(it);
    }
    return it;
  }

  @Override
  public int size() {
    return inner.size();
  }

  @Override
  public String toString() {
    return inner.toString();
  }

  @Override
  public DBID get(int i) {
    return inner.get(i);
  }

  @Override
  public void assignVar(int index, DBIDVar var) {
    inner.assignVar(index, var);
  }

  @Override
  public int binarySearch(DBIDRef key) {
    return inner.binarySearch(key);
  }

  @Override
  public IntegerArrayDBIDs slice(int begin, int end) {
    return new UnmodifiableIntegerArrayDBIDs(inner.slice(begin, end));
  }

  /**
   * Make an existing DBIDMIter unmodifiable.
   * 
   * @author Erich Schubert
   */
  class UnmodifiableDBIDIter implements IntegerDBIDArrayIter {
    /**
     * Wrapped iterator.
     */
    private IntegerDBIDArrayIter it;

    /**
     * Constructor.
     * 
     * @param it inner iterator
     */
    public UnmodifiableDBIDIter(IntegerDBIDArrayIter it) {
      super();
      this.it = it;
    }

    @Override
    public boolean valid() {
      return it.valid();
    }

    @Override
    public void advance() {
      it.advance();
    }

    @Override
    public void advance(int count) {
      it.advance(count);
    }

    @Override
    public void retract() {
      it.retract();
    }

    @Override
    public void seek(int off) {
      it.seek(off);
    }

    @Override
    public int getOffset() {
      return it.getOffset();
    }

    @Override
    public int internalGetIndex() {
      return it.internalGetIndex();
    }

    @Override
    public String toString() {
      return it.toString();
    }
  }
}
