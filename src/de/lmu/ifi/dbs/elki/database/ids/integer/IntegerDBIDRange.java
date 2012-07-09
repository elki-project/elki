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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * Representing a DBID range allocation
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has IntegerDBID
 */
class IntegerDBIDRange implements DBIDRange {
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
  public boolean isEmpty() {
    return len == 0;
  }

  @Override
  public DBIDArrayIter iter() {
    return new DBIDItr();
  }

  /**
   * Iterator in ELKI/C++ style.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class DBIDItr implements DBIDArrayIter, IntegerDBIDRef {
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
    public void advance(int count) {
      pos += count;
    }

    @Override
    public void retract() {
      pos--;
    }

    @Override
    public void seek(int off) {
      pos = off;
    }

    @Override
    public int getOffset() {
      return pos;
    }

    @Override
    public int getIntegerID() {
      return start + pos;
    }

    @Override
    public DBID deref() {
      return new IntegerDBID(start + pos);
    }

    @Override
    public boolean equals(Object other) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return Integer.toString(getIntegerID());
    }
  }

  @Override
  public boolean contains(DBIDRef o) {
    int oid = DBIDFactory.FACTORY.asInteger(o);
    if(oid < start) {
      return false;
    }
    if(oid >= start + len) {
      return false;
    }
    return true;
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
  public int getOffset(DBIDRef dbid) {
    return DBIDFactory.FACTORY.asInteger(dbid) - start;
  }

  @Override
  public int binarySearch(DBIDRef key) {
    int keyid = DBIDFactory.FACTORY.asInteger(key);
    if(keyid < start) {
      return -1;
    }
    final int off = keyid - start;
    if(off < len) {
      return off;
    }
    return -(len + 1);
  }

  @Override
  public String toString() {
    return "[" + start + " to " + (start + len - 1) + "]";
  }
}