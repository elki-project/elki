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

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreIDMap;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableRecordStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * A class to answer representation queries using the stored Array.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @composed - - - DataStoreIDMap
 * @navhas - projectsTo - ArrayRecordStore.StorageAccessor
 */
public class ArrayRecordStore implements WritableRecordStore {
  /**
   * Data array
   */
  private final Object[][] data;

  /**
   * DBID to index map
   */
  private final DataStoreIDMap idmap;

  /**
   * Constructor with existing data
   *
   * @param data Existing data
   * @param idmap Map for array offsets
   */
  public ArrayRecordStore(Object[][] data, DataStoreIDMap idmap) {
    super();
    this.data = data;
    this.idmap = idmap;
  }

  @Override
  public <T> WritableDataStore<T> getStorage(int col, Class<? super T> datatype) {
    // TODO: add type checking safety?
    return new StorageAccessor<>(col);
  }

  /**
   * Actual getter
   *
   * @param id Database ID
   * @param index column index
   * @return current value
   */
  @SuppressWarnings("unchecked")
  protected <T> T get(DBIDRef id, int index) {
    return (T) data[idmap.mapDBIDToOffset(id)][index];
  }

  /**
   * Actual setter
   *
   * @param id Database ID
   * @param index column index
   * @param value New value
   * @return old value
   */
  @SuppressWarnings("unchecked")
  protected <T> T set(DBIDRef id, int index, T value) {
    T ret = (T) data[idmap.mapDBIDToOffset(id)][index];
    data[idmap.mapDBIDToOffset(id)][index] = value;
    return ret;
  }

  /**
   * Access a single record in the given data.
   *
   * @author Erich Schubert
   *
   * @param <T> Object data type to access
   */
  protected class StorageAccessor<T> implements WritableDataStore<T> {
    /**
     * Representation index.
     */
    private final int index;

    /**
     * Constructor.
     *
     * @param index In-record index
     */
    protected StorageAccessor(int index) {
      super();
      this.index = index;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(DBIDRef id) {
      return (T) ArrayRecordStore.this.get(id, index);
    }

    @Override
    public T put(DBIDRef id, T value) {
      return ArrayRecordStore.this.set(id, index, value);
    }

    @Override
    public void destroy() {
      throw new UnsupportedOperationException("ArrayStore record columns cannot (yet) be destroyed.");
    }

    @Override
    public void delete(DBIDRef id) {
      put(id, null);
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException("ArrayStore record columns cannot (yet) be cleared.");
    }
  }

  @Override
  public boolean remove(DBIDRef id) {
    throw new UnsupportedOperationException("ArrayStore records cannot be removed.");
  }
}
