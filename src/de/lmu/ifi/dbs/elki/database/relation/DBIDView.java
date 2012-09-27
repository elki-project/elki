package de.lmu.ifi.dbs.elki.database.relation;

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

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.UpdatableDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;

/**
 * Pseudo-representation that is the object ID itself.
 * 
 * @author Erich Schubert
 */
public class DBIDView extends AbstractHierarchicalResult implements Relation<DBID> {
  /**
   * The database
   */
  private final Database database;

  /**
   * The ids object
   */
  private final DBIDs ids;

  /**
   * Constructor.
   * 
   * @param database
   * @param ids
   */
  public DBIDView(Database database, DBIDs ids) {
    super();
    this.database = database;
    this.ids = DBIDUtil.makeUnmodifiable(ids);
  }

  @Override
  public Database getDatabase() {
    return database;
  }

  @Override
  public DBID get(DBIDRef id) {
    assert (ids.contains(id));
    return DBIDUtil.deref(id);
  }

  @Override
  public void set(DBIDRef id, DBID val) {
    throw new UnsupportedOperationException("DBIDs cannot be changed.");
  }

  @Override
  public void delete(DBIDRef id) {
    if(database instanceof UpdatableDatabase) {
      ((UpdatableDatabase) database).delete(id);
    }
    else {
      throw new UnsupportedOperationException("Deletions are not supported.");
    }
  }

  @Override
  public SimpleTypeInformation<DBID> getDataTypeInformation() {
    return TypeUtil.DBID;
  }

  @Override
  public DBIDs getDBIDs() {
    return ids;
  }

  @Override
  public DBIDIter iterDBIDs() {
    return ids.iter();
  }

  @Override
  public int size() {
    return ids.size();
  }

  @Override
  public String getLongName() {
    return "Database IDs";
  }

  @Override
  public String getShortName() {
    return "DBID";
  }
}