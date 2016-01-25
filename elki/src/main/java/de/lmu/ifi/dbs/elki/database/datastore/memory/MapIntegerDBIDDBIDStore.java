package de.lmu.ifi.dbs.elki.database.datastore.memory;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDBIDDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;

/**
 * Writable data store for double values.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class MapIntegerDBIDDBIDStore implements WritableDBIDDataStore {
  /**
   * Data storage.
   */
  private TIntIntMap map;

  /**
   * Constructor.
   * 
   * @param size Expected size
   */
  public MapIntegerDBIDDBIDStore(int size) {
    super();
    map = new TIntIntHashMap(size, 0.5f, Integer.MIN_VALUE, DBIDUtil.asInteger(DBIDUtil.invalid()));
  }

  @Override
  @Deprecated
  public DBID get(DBIDRef id) {
    return DBIDUtil.importInteger(map.get(DBIDUtil.asInteger(id)));
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
  public DBID put(DBIDRef id, DBID value) {
    return DBIDUtil.importInteger(map.put(DBIDUtil.asInteger(id), DBIDUtil.asInteger(value)));
  }

  @Override
  public void put(DBIDRef id, DBIDRef value) {
    map.put(DBIDUtil.asInteger(id), DBIDUtil.asInteger(value));
  }

  @Override
  public void putDBID(DBIDRef id, DBIDRef value) {
    map.put(DBIDUtil.asInteger(id), DBIDUtil.asInteger(value));
  }

  @Override
  public DBIDVar assignVar(DBIDRef id, DBIDVar var) {
    final int val = map.get(DBIDUtil.asInteger(id));
    DBIDFactory.FACTORY.assignVar(var, val);
    return var;
  }

  @Override
  public void delete(DBIDRef id) {
    map.remove(DBIDUtil.asInteger(id));
  }

  @Override
  public void destroy() {
    map.clear();
    map = null;
  }

  @Override
  public void clear() {
    map.clear();
  }
}
