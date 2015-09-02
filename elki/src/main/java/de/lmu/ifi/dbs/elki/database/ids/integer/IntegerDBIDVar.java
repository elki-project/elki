package de.lmu.ifi.dbs.elki.database.ids.integer;

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

import de.lmu.ifi.dbs.elki.database.datastore.DBIDDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

/**
 * Variable for storing a single DBID reference.
 *
 * TODO: what is the actual memory cost for adding a flag to indicate "null"
 * values, to allow the variable to be unset? Given 8-byte alignment of Java, it
 * should come for free!
 *
 * @author Erich Schubert
 */
class IntegerDBIDVar implements DBIDVar, IntegerDBIDs {
  /**
   * The actual value.
   */
  int id;

  /**
   * Constructor.
   */
  protected IntegerDBIDVar() {
    this.id = Integer.MIN_VALUE;
  }

  /**
   * Constructor.
   *
   * @param val
   */
  protected IntegerDBIDVar(DBIDRef val) {
    this.id = val.internalGetIndex();
  }

  @Override
  public int internalGetIndex() {
    return id;
  }

  /**
   * Internal set to integer.
   *
   * @param i integer value
   */
  protected void internalSetIndex(int i) {
    id = i;
  }

  @Override
  public void set(DBIDRef ref) {
    id = ref.internalGetIndex();
  }

  @Override
  public void setFirst(DBIDPair pair) {
    assert pair instanceof IntegerDBIDPair;
    id = ((IntegerDBIDPair) pair).first;
  }

  @Override
  public void setSecond(DBIDPair pair) {
    assert pair instanceof IntegerDBIDPair;
    id = ((IntegerDBIDPair) pair).second;
  }

  @Override
  @Deprecated
  public DBID get(int i) {
    if(i != 0) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return new IntegerDBID(i);
  }

  @Override
  public int size() {
    return id < 0 ? 0 : 1;
  }

  @Override
  public boolean isEmpty() {
    return id < 0;
  }

  @Override
  public void unset() {
    id = Integer.MIN_VALUE;
  }

  @Override
  public boolean isSet() {
    return id > 0;
  }

  @Override
  public int binarySearch(DBIDRef key) {
    final int other = key.internalGetIndex();
    return (other == id) ? 0 : (other < id) ? -1 : -2;
  }

  @Override
  public boolean contains(DBIDRef o) {
    return id == o.internalGetIndex();
  }

  @Override
  public void assignVar(int i, DBIDVar var) {
    if(var instanceof IntegerDBIDVar) {
      ((IntegerDBIDVar) var).internalSetIndex(i);
    }
    else {
      // Much less efficient:
      var.set(get(i));
    }
  }

  @Override
  public DBIDVar from(DBIDDataStore store, DBIDRef ref) {
    store.assignVar(ref, this);
    return this;
  }

  @Override
  public ArrayDBIDs slice(int begin, int end) {
    if(begin == 0 && end == 1) {
      return this;
    }
    else {
      return DBIDUtil.EMPTYDBIDS;
    }
  }

  @Override
  public String toString() {
    return Integer.toString(id);
  }

  @Override
  public Itr iter() {
    return new Itr();
  }

  /**
   * Pseudo iterator for DBIDs interface.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  protected class Itr implements IntegerDBIDArrayIter, IntegerDBIDRef {
    /**
     * Iterator position: We use an integer so we can support retract().
     */
    int pos = 0;

    @Override
    public Itr advance() {
      pos++;
      return this;
    }

    @Override
    public Itr advance(int count) {
      pos += count;
      return this;
    }

    @Override
    public Itr retract() {
      pos--;
      return this;
    }

    @Override
    public Itr seek(int off) {
      pos = off;
      return this;
    }

    @Override
    public int getOffset() {
      return pos;
    }

    @Override
    public int internalGetIndex() {
      return IntegerDBIDVar.this.id;
    }

    @Override
    public boolean valid() {
      return (pos == 0);
    }

    @Override
    public int hashCode() {
      // Override, because we also are overriding equals.
      return super.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if(other instanceof DBID) {
        LoggingUtil.warning("Programming error detected: DBIDItr.equals(DBID). Use sameDBID()!", new Throwable());
      }
      return super.equals(other);
    }

    @Override
    public String toString() {
      return Integer.toString(internalGetIndex());
    }
  }
}
