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

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreIDMap;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;

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
    super();
    this.data = new double[size];
    this.idmap = idmap;
  }

  @Override
  @Deprecated
  public Double get(DBID id) {
    try {
      return data[idmap.map(id)];
    }
    catch(ArrayIndexOutOfBoundsException e) {
      return null;
    }
  }

  @Override
  @Deprecated
  public Double put(DBID id, Double value) {
    final int off = idmap.map(id);
    double ret = data[off];
    data[off] = value;
    return ret;
  }
  
  @Override
  public double doubleValue(DBID id) {
    return data[idmap.map(id)];
  }

  @Override
  public double putDouble(DBID id, double value) {
    final int off = idmap.map(id);
    final double ret = data[off];
    data[off] = value;
    return ret;
  }

  @Override
  public double put(DBID id, double value) {
    final int off = idmap.map(id);
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