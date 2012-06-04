package de.lmu.ifi.dbs.elki.database.datastore.memory;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreIDMap;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

/**
 * A class to answer representation queries using the stored Array.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf de.lmu.ifi.dbs.elki.database.datastore.DataStoreIDMap
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
    if (def != 0) {
      LoggingUtil.warning("Fill: "+def);
      Arrays.fill(this.data, def);
    }
    this.idmap = idmap;
  }

  @Override
  @Deprecated
  public Integer get(DBID id) {
    try {
      return data[idmap.map(id)];
    }
    catch(ArrayIndexOutOfBoundsException e) {
      return null;
    }
  }

  @Override
  @Deprecated
  public Integer put(DBID id, Integer value) {
    final int off = idmap.map(id);
    int ret = data[off];
    data[off] = value;
    return ret;
  }
  
  @Override
  public int intValue(DBID id) {
    return data[idmap.map(id)];
  }

  @Override
  public int putInt(DBID id, int value) {
    final int off = idmap.map(id);
    final int ret = data[off];
    data[off] = value;
    return ret;
  }

  @Override
  public int put(DBID id, int value) {
    final int off = idmap.map(id);
    final int ret = data[off];
    data[off] = value;
    return ret;
  }

  @Override
  public void destroy() {
    data = null;
    idmap = null;
  }

  @Override
  public void delete(DBID id) {
    throw new UnsupportedOperationException("Can't delete from a static array storage.");
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