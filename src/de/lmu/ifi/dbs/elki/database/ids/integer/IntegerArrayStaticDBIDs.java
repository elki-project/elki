package de.lmu.ifi.dbs.elki.database.ids.integer;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.ids.ArrayStaticDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

/**
 * Static (no modifications allowed) set of Database Object IDs.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has IntegerDBID
 */
public class IntegerArrayStaticDBIDs extends AbstractList<DBID> implements ArrayStaticDBIDs {
  /**
   * The actual storage.
   */
  protected int[] ids;

  /**
   * Constructor
   * 
   * @param ids Array of ids.
   */
  public IntegerArrayStaticDBIDs(int... ids) {
    super();
    this.ids = ids;
  }

  @Override
  public Iterator<DBID> iterator() {
    return new Itr();
  }

  @Override
  public DBIDIter iter() {
    return new DBIDItr();
  }

  /**
   * Iterator class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class Itr implements Iterator<DBID> {
    int off = 0;

    @Override
    public boolean hasNext() {
      return off < ids.length;
    }

    @Override
    public DBID next() {
      DBID ret = new IntegerDBID(ids[off]);
      off++;
      return ret;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * DBID iterator in ELKI/C style.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class DBIDItr implements DBIDIter {
    int pos = 0;

    @Override
    public boolean valid() {
      return pos < ids.length;
    }

    @Override
    public void advance() {
      pos++;
    }

    @Override
    public int getIntegerID() {
      return ids[pos];
    }

    @Override
    public DBID getDBID() {
      return new IntegerDBID(ids[pos]);
    }

    @Override
    public boolean sameDBID(DBIDRef other) {
      return ids[pos] == other.getIntegerID();
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof DBID) {
        LoggingUtil.warning("Programming error detected: DBIDItr.equals(DBID). Use sameDBID()!", new Throwable());
      }
      return super.equals(other);
    }

    @Override
    public int compareDBID(DBIDRef o) {
      int anotherVal = o.getIntegerID();
      return (ids[pos] < anotherVal ? -1 : (ids[pos] == anotherVal ? 0 : 1));
    }
  }

  @Override
  public int size() {
    return ids.length;
  }

  @Override
  public boolean contains(DBIDRef o) {
    final int oid = o.getIntegerID();
    for(int i = 0; i < ids.length; i++) {
      if(ids[i] == oid) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] a) {
    T[] r = a;
    if(a.length < ids.length) {
      r = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), ids.length);
    }
    for(int i = 0; i < ids.length; i++) {
      r[i] = (T) DBIDFactory.FACTORY.importInteger(ids[i]);
    }
    // zero-terminate array
    if(r.length > ids.length) {
      r[ids.length] = null;
    }
    return r;
  }

  @Override
  public DBID get(int i) {
    return DBIDFactory.FACTORY.importInteger(ids[i]);
  }

  @Override
  public int binarySearch(DBIDRef key) {
    return Arrays.binarySearch(ids, key.getIntegerID());
  }
}