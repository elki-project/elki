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

import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * A class to answer representation queries using a map. Basically, it is just a
 * wrapper around a regular map.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <T> Representation object type
 */
public class MapIntegerDBIDStore<T> implements WritableDataStore<T> {
  /**
   * Storage Map.
   */
  private Int2ObjectMap<T> data;

  /**
   * Constructor.
   *
   * @param data Existing map
   */
  public MapIntegerDBIDStore(Int2ObjectMap<T> data) {
    super();
    this.data = data;
  }

  /**
   * Constructor.
   */
  public MapIntegerDBIDStore() {
    super();
    this.data = new Int2ObjectOpenHashMap<>();
  }

  /**
   * Constructor.
   *
   * @param size Expected size
   */
  public MapIntegerDBIDStore(int size) {
    this.data = new Int2ObjectOpenHashMap<>(size);
  }

  @Override
  public T get(DBIDRef id) {
    return data.get(DBIDUtil.asInteger(id));
  }

  @Override
  public T put(DBIDRef id, T value) {
    if(value == null) {
      return data.remove(DBIDUtil.asInteger(id));
    }
    return data.put(DBIDUtil.asInteger(id), value);
  }

  @Override
  public void destroy() {
    data = null;
  }

  @Override
  public void delete(DBIDRef id) {
    data.remove(DBIDUtil.asInteger(id));
  }

  @Override
  public void clear() {
    data.clear();
  }
}
