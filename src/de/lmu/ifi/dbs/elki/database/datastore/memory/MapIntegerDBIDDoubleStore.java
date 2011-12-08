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

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * Writable data store for double values.
 * 
 * @author Erich Schubert
 */
public class MapIntegerDBIDDoubleStore implements WritableDoubleDataStore {
  /**
   * Data storage
   */
  private TIntDoubleMap map;
  
  /**
   * Constructor.
   *
   * @param size Expected size
   */
  public MapIntegerDBIDDoubleStore(int size) {
    super();
    map = new TIntDoubleHashMap(size, 0.5f, Integer.MIN_VALUE, Double.NaN);
  }

  @Override
  @Deprecated
  public Double get(DBID id) {
    return map.get(id.getIntegerID());
  }

  @Override
  public double doubleValue(DBID id) {
    return map.get(id.getIntegerID());
  }

  @Override
  public String getLongName() {
    return "raw";
  }

  @Override
  public String getShortName() {
    return "raw";
  }

  @Override
  @Deprecated
  public Double put(DBID id, Double value) {
    return map.put(id.getIntegerID(), value);
  }

  @Override
  public void destroy() {
    map.clear();
    map = null;
  }

  @Override
  public void delete(DBID id) {
    map.remove(id.getIntegerID());
  }

  @Override
  public double putDouble(DBID id, double value) {
    return map.put(id.getIntegerID(), value);
  }

  @Override
  public double put(DBID id, double value) {
    return map.put(id.getIntegerID(), value);
  }
}
