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

import java.util.Arrays;

import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.DBIDEnum;
import elki.database.ids.DBIDRef;

/**
 * A class to answer representation queries using the stored Array.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @composed - - - elki.database.datastore.DBIDEnum
 */
public class ArrayIntegerStore implements WritableIntegerDataStore {
  /**
   * Data array
   */
  private int[] data;

  /**
   * DBID to index map
   */
  private DBIDEnum idmap;

  /**
   * Default value (for {@link #clear()}).
   */
  private int def;

  /**
   * Constructor.
   *
   * @param idmap ID map
   */
  public ArrayIntegerStore(DBIDEnum idmap) {
    this(idmap, 0);
  }

  /**
   * Constructor.
   *
   * @param idmap ID map
   * @param def Default value
   */
  public ArrayIntegerStore(DBIDEnum idmap, int def) {
    super();
    final int size = idmap.size();
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
    return Integer.valueOf(data[idmap.index(id)]);
  }

  @Override
  @Deprecated
  public Integer put(DBIDRef id, Integer value) {
    final int off = idmap.index(id);
    int ret = data[off];
    data[off] = value.intValue();
    return Integer.valueOf(ret);
  }

  @Override
  public int intValue(DBIDRef id) {
    return data[idmap.index(id)];
  }

  @Override
  public int putInt(DBIDRef id, int value) {
    final int off = idmap.index(id);
    final int ret = data[off];
    data[off] = value;
    return ret;
  }

  @Override
  public int put(DBIDRef id, int value) {
    final int off = idmap.index(id);
    final int ret = data[off];
    data[off] = value;
    return ret;
  }

  @Override
  public void increment(DBIDRef id, int adjust) {
    final int off = idmap.index(id);
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
