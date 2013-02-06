package de.lmu.ifi.dbs.elki.database.datastore.memory;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDistanceDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Writable data store for double values.
 * 
 * @author Erich Schubert
 */
public class MapIntegerDBIDDoubleDistanceStore implements WritableDoubleDistanceDataStore {
  /**
   * Data storage.
   */
  private TIntDoubleMap map;

  /**
   * Constructor.
   * 
   * @param size Expected size
   */
  public MapIntegerDBIDDoubleDistanceStore(int size) {
    this(size, Double.NaN);
  }

  /**
   * Constructor.
   * 
   * @param size Expected size
   * @param def Default value
   */
  public MapIntegerDBIDDoubleDistanceStore(int size, double def) {
    super();
    map = new TIntDoubleHashMap(size, 0.5f, Integer.MIN_VALUE, def);
  }

  @Override
  @Deprecated
  public DoubleDistance get(DBIDRef id) {
    return new DoubleDistance(map.get(DBIDUtil.asInteger(id)));
  }

  @Override
  public double doubleValue(DBIDRef id) {
    return map.get(DBIDUtil.asInteger(id));
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
  public DoubleDistance put(DBIDRef id, DoubleDistance value) {
    return new DoubleDistance(map.put(DBIDUtil.asInteger(id), value.doubleValue()));
  }

  @Override
  public void destroy() {
    map.clear();
    map = null;
  }

  @Override
  public void delete(DBIDRef id) {
    map.remove(DBIDUtil.asInteger(id));
  }

  @Override
  public double putDouble(DBIDRef id, double value) {
    return map.put(DBIDUtil.asInteger(id), value);
  }

  @Override
  public double put(DBIDRef id, double value) {
    return map.put(DBIDUtil.asInteger(id), value);
  }
}
