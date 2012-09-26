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
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * A class to answer representation queries using the stored Array.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf de.lmu.ifi.dbs.elki.database.datastore.DataStoreIDMap
 */
public class ArrayDoubleStore implements WritableDoubleDataStore {
  /**
   * Data array
   */
  private double[] data;

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
  public ArrayDoubleStore(int size, DataStoreIDMap idmap) {
    this(size, idmap, Double.NaN);
  }

  /**
   * Constructor.
   * 
   * @param size Size
   * @param idmap ID map
   * @param def Default value
   */
  public ArrayDoubleStore(int size, DataStoreIDMap idmap, double def) {
    super();
    this.data = new double[size];
    if(def != 0) {
      Arrays.fill(this.data, def);
    }
    this.idmap = idmap;
  }

  @Override
  @Deprecated
  public Double get(DBIDRef id) {
    try {
      return data[idmap.mapDBIDToOffset(id)];
    }
    catch(ArrayIndexOutOfBoundsException e) {
      return null;
    }
  }

  @Override
  @Deprecated
  public Double put(DBIDRef id, Double value) {
    final int off = idmap.mapDBIDToOffset(id);
    double ret = data[off];
    data[off] = value;
    return ret;
  }

  @Override
  public double doubleValue(DBIDRef id) {
    return data[idmap.mapDBIDToOffset(id)];
  }

  @Override
  public double putDouble(DBIDRef id, double value) {
    final int off = idmap.mapDBIDToOffset(id);
    final double ret = data[off];
    data[off] = value;
    return ret;
  }

  @Override
  public double put(DBIDRef id, double value) {
    final int off = idmap.mapDBIDToOffset(id);
    final double ret = data[off];
    data[off] = value;
    return ret;
  }

  @Override
  public void destroy() {
    data = null;
    idmap = null;
  }

  @Override
  public void delete(DBIDRef id) {
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