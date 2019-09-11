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
package elki.database.relation;

import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.DoubleDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.logging.Logging;

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
   * Constructor.
   *
   * @param name Name
   * @param ids IDs
   */
  public MaterializedDoubleRelation(String name, DBIDs ids) {
    this(name, ids, DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB));
  }

  /**
   * Constructor.
   *
   * @param name Name
   * @param ids IDs
   * @param content Content
   */
  public MaterializedDoubleRelation(String name, DBIDs ids, DoubleDataStore content) {
    super();
    this.ids = DBIDUtil.makeUnmodifiable(ids);
    this.name = name;
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
  protected Logging getLogger() {
    return LOG;
  }
}