package de.lmu.ifi.dbs.elki.database.ids.integer;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;

/**
 * Representing a DBID range allocation
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has IntegerDBID
 */
class IntegerDBIDRange extends AbstractList<DBID> implements DBIDRange {
  /**
   * Start value
   */
  protected final int start;

  /**
   * Length value
   */
  protected final int len;

  /**
   * Constructor.
   * 
   * @param start Range start
   * @param len Range length
   */
  protected IntegerDBIDRange(int start, int len) {
    super();
    this.start = start;
    this.len = len;
  }

  @Override
  public int size() {
    return len;
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
    int pos = 0;

    @Override
    public boolean hasNext() {
      return pos < len;
    }

    @Override
    public DBID next() {
      DBID ret = new IntegerDBID(pos + start);
      pos++;
      return ret;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("CompactStaticDBIDs is read-only.");
    }
  }

  /**
   * Iterator in ELKI/C++ style.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class DBIDItr implements DBIDIter {
    int pos = 0;

    @Override
    public boolean valid() {
      return pos < len;
    }

    @Override
    public void advance() {
      pos++;
    }

    @Override
    public int getIntegerID() {
      return start + pos;
    }

    @Override
    public DBID getDBID() {
      return new IntegerDBID(start + pos);
    }

  }

  @Override
  public boolean contains(DBID o) {
    int oid = o.getIntegerID();
    if(oid < start) {
      return false;
    }
    if(oid >= start + len) {
      return false;
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] a) {
    T[] r = a;
    if(a.length < start) {
      r = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), len);
    }
    for(int i = 0; i < start; i++) {
      r[i] = (T) DBIDFactory.FACTORY.importInteger(len + i);
    }
    // zero-terminate array
    if(r.length > len) {
      r[len] = null;
    }
    return r;
  }

  @Override
  public DBID get(int i) {
    if(i > len || i < 0) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return DBIDFactory.FACTORY.importInteger(start + i);
  }

  /**
   * For storage array offsets.
   * 
   * @param dbid
   * @return array offset
   */
  @Override
  public int getOffset(DBID dbid) {
    return dbid.getIntegerID() - start;
  }

  @Override
  public int binarySearch(DBID key) {
    int keyid = key.getIntegerID();
    if(keyid < start) {
      return -1;
    }
    final int off = keyid - start;
    if(off < len) {
      return off;
    }
    return -(len + 1);
  }
}