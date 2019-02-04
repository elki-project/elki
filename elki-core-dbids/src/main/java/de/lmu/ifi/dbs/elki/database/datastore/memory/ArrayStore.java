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
package de.lmu.ifi.dbs.elki.database.datastore.memory;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreIDMap;
import de.lmu.ifi.dbs.elki.database.datastore.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;

/**
 * A class to answer representation queries using the stored Array.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @composed - - - de.lmu.ifi.dbs.elki.database.datastore.DataStoreIDMap
 *
 * @param <T> Representation object typ
 */
public class ArrayStore<T> implements WritableDataStore<T> {
  /**
   * Data array.
   */
  private Object[] data;

  /**
   * DBID to index map.
   */
  private DataStoreIDMap idmap;

  /**
   * Constructor.
   *
   * @param data Data array
   * @param idmap DBID to offset mapping
   */
  public ArrayStore(Object[] data, DataStoreIDMap idmap) {
    super();
    this.data = data;
    this.idmap = idmap;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T get(DBIDRef id) {
    final int off = idmap.mapDBIDToOffset(id);
    if(off < 0 || off >= data.length) {
      throw new ObjectNotFoundException(DBIDUtil.deref(id));
    }
    return (T) data[off];
  }

  @Override
  public T put(DBIDRef id, T value) {
    T ret = get(id);
    data[idmap.mapDBIDToOffset(id)] = value;
    return ret;
  }

  @Override
  public void destroy() {
    data = null;
    idmap = null;
  }

  @Override
  public void delete(DBIDRef id) {
    put(id, null);
  }

  @Override
  public void clear() {
    Arrays.fill(data, null);
  }
}
