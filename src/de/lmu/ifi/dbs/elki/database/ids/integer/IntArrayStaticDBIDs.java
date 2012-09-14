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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

/**
 * Static (no modifications allowed) set of Database Object IDs.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has IntegerDBID
 */
public class IntArrayStaticDBIDs implements IntegerArrayStaticDBIDs {
  /**
   * The actual storage.
   */
  protected int[] ids;

  /**
   * Constructor.
   * 
   * @param ids Array of ids.
   */
  public IntArrayStaticDBIDs(int... ids) {
    super();
    this.ids = ids;
  }

  @Override
  public IntegerDBIDArrayIter iter() {
    return new DBIDItr();
  }

  /**
   * DBID iterator in ELKI/C style.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class DBIDItr implements IntegerDBIDArrayIter {
    /**
     * Position within array.
     */
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
    public void advance(int count) {
      pos += 0;
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
      return ids[pos];
    }

    @Override
    public DBID deref() {
      return new IntegerDBID(ids[pos]);
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
      return Integer.toString(getIntegerID());
    }
  }

  @Override
  public int size() {
    return ids.length;
  }

  @Override
  public boolean isEmpty() {
    return ids.length == 0;
  }

  @Override
  public boolean contains(DBIDRef o) {
    final int oid = DBIDFactory.FACTORY.asInteger(o);
    for(int i = 0; i < ids.length; i++) {
      if(ids[i] == oid) {
        return true;
      }
    }
    return false;
  }

  @Override
  public DBID get(int i) {
    return DBIDFactory.FACTORY.importInteger(ids[i]);
  }

  @Override
  public int binarySearch(DBIDRef key) {
    return Arrays.binarySearch(ids, DBIDFactory.FACTORY.asInteger(key));
  }
}