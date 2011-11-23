package de.lmu.ifi.dbs.elki.database.ids.integer;

import gnu.trove.set.hash.TIntHashSet;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;

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

/**
 * Implementation using GNU Trove Int Hash Sets.
 * 
 * @author Erich Schubert
 * 
 */
class TroveHashSetModifiableDBIDs extends TroveSetDBIDs implements HashSetModifiableDBIDs {
  /**
   * The actual store.
   */
  TIntHashSet store;

  /**
   * Constructor.
   * 
   * @param size Initial size
   */
  protected TroveHashSetModifiableDBIDs(int size) {
    super();
    this.store = new TIntHashSet(size);
  }

  /**
   * Constructor.
   */
  protected TroveHashSetModifiableDBIDs() {
    super();
    this.store = new TIntHashSet();
  }

  /**
   * Constructor.
   *
   * @param existing Existing IDs
   */
  protected TroveHashSetModifiableDBIDs(DBIDs existing) {
    this(existing.size());
    this.addDBIDs(existing);
  }

  @Override
  public boolean addDBIDs(DBIDs ids) {
    boolean success = false;
    for(DBID id : ids) {
      success |= store.add(id.getIntegerID());
    }
    return success;
  }

  @Override
  public boolean removeDBIDs(DBIDs ids) {
    boolean success = false;
    for(DBID id : ids) {
      success |= store.remove(id.getIntegerID());
    }
    return success;
  }

  @Override
  public boolean add(DBID e) {
    return store.add(e.getIntegerID());
  }

  @Override
  public boolean remove(Object o) {
    return store.remove(((DBID) o).getIntegerID());
  }

  @Override
  public boolean retainAll(DBIDs set) {
    return retainAll(set.asCollection());
  }

  @Override
  protected TIntHashSet getStore() {
    return store;
  }
}