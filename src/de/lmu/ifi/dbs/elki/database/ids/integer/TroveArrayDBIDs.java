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

import gnu.trove.list.TIntList;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

/**
 * Abstract base class for GNU Trove array based lists.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has IntegerDBID
 * @apiviz.has TroveIteratorAdapter
 */
public abstract class TroveArrayDBIDs implements ArrayDBIDs {
  /**
   * Get the array store
   * 
   * @return the store
   */
  abstract protected TIntList getStore();

  @Override
  public DBIDMIter iter() {
    return new DBIDItr(getStore());
  }

  @Override
  public DBID get(int index) {
    return new IntegerDBID(getStore().get(index));
  }

  @Override
  public int size() {
    return getStore().size();
  }
  
  @Override
  public boolean isEmpty() {
    return getStore().isEmpty();
  }

  @Override
  public boolean contains(DBIDRef o) {
    return getStore().contains(DBIDFactory.FACTORY.asInteger(o));
  }

  @Override
  public int binarySearch(DBIDRef key) {
    return getStore().binarySearch(DBIDFactory.FACTORY.asInteger(key));
  }

  /**
   * Iterate over a Trove IntList, ELKI/C-style
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected static class DBIDItr implements DBIDMIter, IntegerDBIDRef {
    /**
     * Current position
     */
    int pos = 0;

    /**
     * The actual store we use
     */
    TIntList store;

    /**
     * Constructor.
     * 
     * @param store The actual trove store
     */
    public DBIDItr(TIntList store) {
      super();
      this.store = store;
    }

    @Override
    public boolean valid() {
      return pos < store.size();
    }

    @Override
    public void advance() {
      pos++;
    }

    @Override
    public int getIntegerID() {
      return store.get(pos);
    }

    @Override
    public DBID deref() {
      return new IntegerDBID(store.get(pos));
    }

    @Override
    public void remove() {
      store.removeAt(pos);
      pos--;
    }
    
    @Override
    public boolean equals(Object other) {
      if (other instanceof DBID) {
        LoggingUtil.warning("Programming error detected: DBIDItr.equals(DBID). Use sameDBID()!", new Throwable());
      }
      return super.equals(other);
    }
  }
}