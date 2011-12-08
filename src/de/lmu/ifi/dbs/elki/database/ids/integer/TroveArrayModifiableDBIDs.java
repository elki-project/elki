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

import gnu.trove.list.array.TIntArrayList;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Class using a GNU Trove int array list as storage.
 * 
 * @author Erich Schubert
 */
class TroveArrayModifiableDBIDs extends TroveArrayDBIDs implements ArrayModifiableDBIDs {
  /**
   * The actual trove array list
   */
  private TIntArrayList store;

  /**
   * Constructor.
   * 
   * @param size Initial size
   */
  protected TroveArrayModifiableDBIDs(int size) {
    super();
    this.store = new TIntArrayList(size);
  }

  /**
   * Constructor.
   */
  protected TroveArrayModifiableDBIDs() {
    super();
    this.store = new TIntArrayList();
  }

  /**
   * Constructor.
   *
   * @param existing Existing ids
   */
  protected TroveArrayModifiableDBIDs(DBIDs existing) {
    this(existing.size());
    this.addDBIDs(existing);
  }

  @Override
  protected TIntArrayList getStore() {
    return store;
  }

  @Override
  public boolean addDBIDs(DBIDs ids) {
    boolean success = false;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      success |= store.add(iter.getIntegerID());
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
  public DBID set(int index, DBID element) {
    int prev = store.set(index, element.getIntegerID());
    return new IntegerDBID(prev);
  }

  @Override
  public void add(int index, DBID element) {
    store.insert(index, element.getIntegerID());
  }

  @Override
  public DBID remove(int index) {
    return new IntegerDBID(store.removeAt(index));
  }

  @Override
  public void sort() {
    store.sort();
  }
}