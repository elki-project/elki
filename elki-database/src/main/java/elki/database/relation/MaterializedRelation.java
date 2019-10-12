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
import elki.database.datastore.DataStore;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.*;
import elki.index.DynamicIndex;
import elki.index.Index;
import elki.result.Metadata;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.exceptions.AbortException;

/**
 * Represents a single representation. This is attached to a DBIDs object, which
 * you are supposed to manage first. I.e. put the new DBID in, then invoke
 * set(), remove the DBID, then delete().
 * <p>
 * TODO: is this semantic sane?
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @param <O> Data type
 */
public class MaterializedRelation<O> implements ModifiableRelation<O> {
  /**
   * The class of objects we store.
   */
  private final SimpleTypeInformation<O> type;

  /**
   * Map to hold the objects of the database.
   */
  private final DataStore<O> content;

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
   * @param type Type information
   * @param ids IDs
   */
  public MaterializedRelation(SimpleTypeInformation<O> type, DBIDs ids) {
    this(null, type, ids);
  }

  /**
   * Constructor.
   * 
   * @param name Name
   * @param type Type information
   * @param ids IDs
   */
  public MaterializedRelation(String name, SimpleTypeInformation<O> type, DBIDs ids) {
    // We can't call this() since we'll have generics issues then.
    super();
    this.type = type;
    this.ids = DBIDUtil.makeUnmodifiable(ids);
    this.name = name;
    this.content = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, type.getRestrictionClass());
  }

  /**
   * Constructor.
   * 
   * @param name Name
   * @param type Type information
   * @param ids IDs
   * @param content Content
   */
  public MaterializedRelation(String name, SimpleTypeInformation<O> type, DBIDs ids, DataStore<O> content) {
    super();
    this.type = type;
    this.ids = DBIDUtil.makeUnmodifiable(ids);
    this.name = name;
    this.content = content;
  }

  @Override
  public O get(DBIDRef id) {
    return content.get(id);
  }

  @Override
  public void insert(DBIDRef id, O val) {
    assert (ids.contains(id)) : "Object not yet in DBIDs.";
    if(!(content instanceof WritableDataStore)) {
      throw new AbortException("Data is stored in a non-writable data store. Modifications are not possible.");
    }
    ((WritableDataStore<O>) content).put(id, val);
    for(It<Index> it = Metadata.hierarchyOf(this).iterDescendants().filter(Index.class); it.valid(); it.advance()) {
      if(!(it.get() instanceof DynamicIndex)) {
        throw new AbortException("A non-dynamic index was added to this database. Modifications are not allowed, unless this index is removed.");
      }
      ((DynamicIndex) it.get()).insert(id);
    }
  }

  /**
   * Delete an objects values.
   *
   * @param id ID to delete
   */
  @Override
  public void delete(DBIDRef id) {
    assert (!ids.contains(id)) : "Object still in DBIDs.";
    if(!(content instanceof WritableDataStore)) {
      throw new AbortException("Data is stored in a non-writable data store. Modifications are not possible.");
    }
    for(It<Index> it = Metadata.hierarchyOf(this).iterDescendants().filter(Index.class); it.valid(); it.advance()) {
      if(!(it.get() instanceof DynamicIndex)) {
        throw new AbortException("A non-dynamic index was added to this database. Modifications are not allowed, unless this index is removed.");
      }
      ((DynamicIndex) it.get()).delete(id);
    }
    ((WritableDataStore<O>) content).delete(id);
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
  public SimpleTypeInformation<O> getDataTypeInformation() {
    return type;
  }

  @Override
  public String getLongName() {
    return name != null ? name : type.toString();
  }
}
