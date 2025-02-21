/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.database.datastore.memory;

import elki.database.datastore.WritableDBIDDataStore;
import elki.database.ids.*;

/**
 * A class to answer representation queries using the stored Array.
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @composed - - - elki.database.datastore.DBIDEnum
 */
public class ArrayDBIDStore implements WritableDBIDDataStore {
  /**
   * Data array
   */
  private ArrayModifiableDBIDs data;

  /**
   * DBID to index map
   */
  private DBIDEnum idmap;

  /**
   * Constructor.
   *
   * @param idmap ID map
   */
  public ArrayDBIDStore(DBIDEnum idmap) {
    super();
    final int size = idmap.size();
    this.data = DBIDUtil.newArray(size);
    // Initialize
    DBIDRef inv = DBIDUtil.invalid();
    for(int i = 0; i < size; i++) {
      data.add(inv);
    }
    this.idmap = idmap;
  }

  @Override
  @Deprecated
  public DBID get(DBIDRef id) {
    return data.get(idmap.index(id));
  }

  @Override
  public DBIDVar assignVar(DBIDRef id, DBIDVar var) {
    return data.assignVar(idmap.index(id), var);
  }

  @Override
  @Deprecated
  public DBID put(DBIDRef id, DBID value) {
    final int off = idmap.index(id);
    DBID ret = data.get(off);
    data.set(off, value);
    return ret;
  }

  @Override
  public void putDBID(DBIDRef id, DBIDRef value) {
    data.set(idmap.index(id), value);
  }

  @Override
  public void put(DBIDRef id, DBIDRef value) {
    data.set(idmap.index(id), value);
  }

  @Override
  public void destroy() {
    data = null;
    idmap = null;
  }

  @Override
  public void clear() {
    // Re-initialize
    DBIDRef inv = DBIDUtil.invalid();
    final int size = data.size();
    data.clear();
    for(int i = 0; i < size; i++) {
      data.add(inv);
    }
  }

  @Override
  public void delete(DBIDRef id) {
    put(id, DBIDUtil.invalid());
  }

  @Override
  public String toString() {
    return data.toString();
  }
}
