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
package de.lmu.ifi.dbs.elki.database.relation;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.StaticDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Represents a single representation. This is attached to a DBIDs object, which
 * you are supposed to manage first. I.e. put the new DBID in, then invoke
 * set(), remove the DBID, then delete().
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public class MaterializedDoubleRelation extends AbstractRelation<Double>implements DoubleRelation {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(MaterializedDoubleRelation.class);

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
   * @param ids IDs
   */
  public MaterializedDoubleRelation(DBIDs ids) {
    this(ids, null);
  }

  /**
   * Constructor.
   *
   * @param ids IDs
   * @param name Name
   */
  public MaterializedDoubleRelation(DBIDs ids, String name) {
    this(ids, name, DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB));
  }

  /**
   * Constructor.
   *
   * @param ids IDs
   * @param name Name
   * @param content Content
   */
  public MaterializedDoubleRelation(DBIDs ids, String name, DoubleDataStore content) {
    super();
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
    this.ids = DBIDUtil.makeUnmodifiable(ids);
    this.name = name;
    this.shortname = shortname;
    this.content = content;
  }

  @Override
  public double doubleValue(DBIDRef id) {
    return content.doubleValue(id);
  }

  @Override
  public void set(DBIDRef id, double val) {
    assert(ids.contains(id));
    if(content instanceof WritableDoubleDataStore) {
      ((WritableDoubleDataStore) content).putDouble(id, val);
    }
  }

  @Deprecated
  @Override
  public void insert(DBIDRef id, Double val) {
    assert(ids.contains(id));
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
    assert(!ids.contains(id));
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

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}