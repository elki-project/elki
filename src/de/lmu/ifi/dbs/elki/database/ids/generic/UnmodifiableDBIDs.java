package de.lmu.ifi.dbs.elki.database.ids.generic;

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

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.StaticDBIDs;

/**
 * Unmodifiable wrapper for DBIDs.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses DBIDs
 * @apiviz.has UnmodifiableDBIDIter
 */
public class UnmodifiableDBIDs implements StaticDBIDs {
  /**
   * The DBIDs we wrap.
   */
  private final DBIDs inner;

  /**
   * Constructor.
   * 
   * @param inner Inner DBID collection.
   */
  public UnmodifiableDBIDs(DBIDs inner) {
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
  public DBIDIter iter() {
    DBIDIter it = inner.iter();
    if(it instanceof DBIDMIter) {
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

  /**
   * Make an existing DBIDMIter unmodifiable.
   * 
   * @author Erich Schubert
   */
  class UnmodifiableDBIDIter implements DBIDIter {
    /**
     * Wrapped iterator.
     */
    private DBIDIter it;

    /**
     * Constructor.
     * 
     * @param it inner iterator
     */
    public UnmodifiableDBIDIter(DBIDIter it) {
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
    public int internalGetIndex() {
      return it.internalGetIndex();
    }
  }
}