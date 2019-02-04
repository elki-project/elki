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
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.StaticDBIDs;

/**
 * Unmodifiable wrapper for DBIDs.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @assoc - - - IntegerDBIDs
 * @has - - - UnmodifiableDBIDIter
 */
public class UnmodifiableIntegerDBIDs implements StaticDBIDs, IntegerDBIDs {
  /**
   * The DBIDs we wrap.
   */
  private final IntegerDBIDs inner;

  /**
   * Constructor.
   * 
   * @param inner Inner DBID collection.
   */
  public UnmodifiableIntegerDBIDs(IntegerDBIDs inner) {
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
  public IntegerDBIDIter iter() {
    IntegerDBIDIter it = inner.iter();
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

  /**
   * Make an existing DBIDMIter unmodifiable.
   * 
   * @author Erich Schubert
   */
  class UnmodifiableDBIDIter implements IntegerDBIDIter {
    /**
     * Wrapped iterator.
     */
    private IntegerDBIDIter it;

    /**
     * Constructor.
     * 
     * @param it inner iterator
     */
    public UnmodifiableDBIDIter(IntegerDBIDIter it) {
      super();
      this.it = it;
    }

    @Override
    public boolean valid() {
      return it.valid();
    }

    @Override
    public DBIDIter advance() {
      it.advance();
      return this;
    }

    @Override
    public int internalGetIndex() {
      return it.internalGetIndex();
    }
  }
}
