package de.lmu.ifi.dbs.elki.database.datastore.memory;

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

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * A class to answer representation queries using a map. Basically, it is just a
 * wrapper around a regular map.
 * 
 * @author Erich Schubert
 * 
 * @param <T> Representation object type
 */
public class MapIntegerDBIDStore<T> implements WritableDataStore<T> {
  /**
   * Storage Map
   */
  private TIntObjectMap<T> data;

  /**
   * Constructor.
   * 
   * @param data Existing map
   */
  public MapIntegerDBIDStore(TIntObjectMap<T> data) {
    super();
    this.data = data;
  }

  /**
   * Constructor.
   */
  public MapIntegerDBIDStore() {
    super();
    this.data = new TIntObjectHashMap<T>();
  }

  @Override
  public T get(DBID id) {
    return data.get(id.getIntegerID());
  }

  @Override
  public T put(DBID id, T value) {
    if(value == null) {
      return data.remove(id.getIntegerID());
    }
    return data.put(id.getIntegerID(), value);
  }

  @Override
  public void destroy() {
    data = null;
  }

  @Override
  public void delete(DBID id) {
    data.remove(id.getIntegerID());
  }

  @Override
  public String getLongName() {
    return "raw";
  }

  @Override
  public String getShortName() {
    return "raw";
  }
}