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
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * A class to answer representation queries using the stored Array.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @composed - - - de.lmu.ifi.dbs.elki.database.datastore.DataStoreIDMap
 */
public class ArrayIntegerStore implements WritableIntegerDataStore {
  /**
   * Data array
   */
  private int[] data;

  /**
   * DBID to index map
   */
  private DataStoreIDMap idmap;

  /**
   * Default value (for {@link #clear()}).
   */
  private int def;

  /**
   * Constructor.
   *
   * @param size Size
   * @param idmap ID map
   */
  public ArrayIntegerStore(int size, DataStoreIDMap idmap) {
    this(size, idmap, 0);
  }

  /**
   * Constructor.
   *
   * @param size Size
   * @param idmap ID map
   * @param def Default value
   */
  public ArrayIntegerStore(int size, DataStoreIDMap idmap, int def) {
    super();
    this.data = new int[size];
    this.def = def;
    if(def != 0) {
      Arrays.fill(this.data, def);
    }
    this.idmap = idmap;
  }

  @Override
  @Deprecated
  public Integer get(DBIDRef id) {
    return Integer.valueOf(data[idmap.mapDBIDToOffset(id)]);
  }

  @Override
  @Deprecated
  public Integer put(DBIDRef id, Integer value) {
    final int off = idmap.mapDBIDToOffset(id);
    int ret = data[off];
    data[off] = value.intValue();
    return Integer.valueOf(ret);
  }

  @Override
  public int intValue(DBIDRef id) {
    return data[idmap.mapDBIDToOffset(id)];
  }

  @Override
  public int putInt(DBIDRef id, int value) {
    final int off = idmap.mapDBIDToOffset(id);
    final int ret = data[off];
    data[off] = value;
    return ret;
  }

  @Override
  public int put(DBIDRef id, int value) {
    final int off = idmap.mapDBIDToOffset(id);
    final int ret = data[off];
    data[off] = value;
    return ret;
  }

  @Override
  public void increment(DBIDRef id, int adjust) {
    final int off = idmap.mapDBIDToOffset(id);
    data[off] += adjust;
  }

  @Override
  public void destroy() {
    data = null;
    idmap = null;
  }

  @Override
  public void delete(DBIDRef id) {
    put(id, def);
  }

  @Override
  public void clear() {
    Arrays.fill(data, def);
  }
}
