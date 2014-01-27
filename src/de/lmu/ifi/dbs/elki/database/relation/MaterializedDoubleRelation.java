package de.lmu.ifi.dbs.elki.database.relation;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.StaticDBIDs;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;

/**
 * Represents a single representation. This is attached to a DBIDs object, which
 * you are supposed to manage first. I.e. put the new DBID in, then invoke
 * set(), remove the DBID, then delete().
 * 
 * @author Erich Schubert
 */
public class MaterializedDoubleRelation extends AbstractHierarchicalResult implements DoubleRelation {
  /**
   * Our database
   */
  private final Database database;

  /**
   * Map to hold the objects of the database.
   */
  private final DoubleDataStore content;

  /**
   * The DBIDs this is supposed to be defined for.
   * 
   * Note: we only keep an unmodifiable reference.
   */
  private final StaticDBIDs ids;

  /**
   * The relation name.
   */
  private String name;

  /**
   * The relation name (short version)
   */
  private String shortname = "relation";

  /**
   * Constructor.
   * 
   * @param database Database
   * @param type Type information
   * @param ids IDs
   */
  public MaterializedDoubleRelation(Database database, DBIDs ids) {
    this(database, ids, null);
  }

  /**
   * Constructor.
   * 
   * @param database Database
   * @param ids IDs
   * @param name Name
   */
  public MaterializedDoubleRelation(Database database, DBIDs ids, String name) {
    // We can't call this() since we'll have generics issues then.
    super();
    this.database = database;
    this.ids = DBIDUtil.makeUnmodifiable(ids);
    this.name = name;
    this.content = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
  }

  /**
   * Constructor.
   * 
   * @param database Database
   * @param ids IDs
   * @param name Name
   * @param content Content
   */
  public MaterializedDoubleRelation(Database database, DBIDs ids, String name, DoubleDataStore content) {
    super();
    this.database = database;
    this.ids = DBIDUtil.makeUnmodifiable(ids);
    this.name = name;
    this.content = content;
  }

  /**
   * Constructor.
   * 
   * @param name Name
   * @param shortname Short name of the result
   * @param content Content
   * @param ids IDs
   */
  public MaterializedDoubleRelation(String name, String shortname, DoubleDataStore content, DBIDs ids) {
    super();
    this.database = null;
    this.ids = DBIDUtil.makeUnmodifiable(ids);
    this.name = name;
    this.shortname = shortname;
    this.content = content;
  }

  @Override
  public Database getDatabase() {
    return database;
  }

  @Override
  public Double get(DBIDRef id) {
    return content.doubleValue(id);
  }

  @Override
  public double doubleValue(DBIDRef id) {
    return content.doubleValue(id);
  }

  @Override
  public void set(DBIDRef id, double val) {
    assert (ids.contains(id));
    if(content instanceof WritableDoubleDataStore) {
      ((WritableDoubleDataStore) content).putDouble(id, val);
    }
  }

  @Override
  public void set(DBIDRef id, Double val) {
    assert (ids.contains(id));
    if(content instanceof WritableDoubleDataStore) {
      ((WritableDoubleDataStore) content).putDouble(id, val);
    }
  }

  /**
   * Delete an objects values.
   * 
   * @param id ID to delete
   */
  @Override
  public void delete(DBIDRef id) {
    assert (!ids.contains(id));
    if(content instanceof WritableDoubleDataStore) {
      ((WritableDoubleDataStore) content).delete(id);
    }
  }

  @Override
  public StaticDBIDs getDBIDs() {
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
  public SimpleTypeInformation<Double> getDataTypeInformation() {
    return TypeUtil.DOUBLE;
  }

  @Override
  public String getLongName() {
    return (name != null) ? name : "Double";
  }

  @Override
  public String getShortName() {
    return shortname;
  }
}