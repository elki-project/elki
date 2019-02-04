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
package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.DBIDView;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.ModifiableRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.bundle.ObjectBundle;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Database storing data using hashtable storage, and thus allowing additional
 * and removal of objects.
 *
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.1
 *
 * @opt nodefillcolor LemonChiffon
 * @composed - - - HashSetModifiableDBIDs
 * @assoc - - - DatabaseConnection
 */
@Description("Database using an in-memory hashtable and at least providing linear scans.")
public class HashmapDatabase extends AbstractDatabase implements UpdatableDatabase {
  /**
   * Our logger
   */
  private static final Logging LOG = Logging.getLogger(HashmapDatabase.class);

  /**
   * IDs of this database
   */
  private HashSetModifiableDBIDs ids;

  /**
   * The DBID representation we use
   */
  private final DBIDView idrep;

  /**
   * The data source we get the initial data from.
   */
  protected DatabaseConnection databaseConnection;

  /**
   * Constructor.
   *
   * @param databaseConnection Database connection to get the initial data from.
   * @param indexFactories Indexes to add
   */
  public HashmapDatabase(DatabaseConnection databaseConnection, Collection<? extends IndexFactory<?>> indexFactories) {
    super();
    this.databaseConnection = databaseConnection;
    this.ids = DBIDUtil.newHashSet();
    this.idrep = new DBIDView(this.ids);
    this.relations.add(idrep);
    this.addChildResult(idrep);

    // Add indexes.
    if(indexFactories != null) {
      this.indexFactories.addAll(indexFactories);
    }
  }

  /**
   * Constructor with no indexes.
   */
  public HashmapDatabase() {
    this(null, null);
  }

  /**
   * Initialize the database by getting the initial data from the database
   * connection.
   */
  @Override
  public void initialize() {
    if(databaseConnection != null) {
      this.insert(databaseConnection.loadData());
      // Run at most once.
      databaseConnection = null;
    }
  }

  @Override
  public DBIDs insert(ObjectBundle objpackages) {
    if(objpackages.dataLength() == 0) {
      return DBIDUtil.EMPTYDBIDS;
    }
    // insert into db
    ArrayModifiableDBIDs newids = DBIDUtil.newArray(objpackages.dataLength());
    Relation<?>[] targets = alignColumns(objpackages);

    DBIDVar var = DBIDUtil.newVar();
    for(int j = 0; j < objpackages.dataLength(); j++) {
      // insert object
      if(!objpackages.assignDBID(j, var)) {
        var.set(DBIDUtil.generateSingleDBID());
      }
      if(ids.contains(var)) {
        throw new AbortException("Duplicate DBID conflict.");
      }
      ids.add(var);
      for(int i = 0; i < targets.length; i++) {
        if(!(targets[i] instanceof ModifiableRelation)) {
          throw new AbortException("Non-modifiable relations have been added to the database.");
        }
        @SuppressWarnings("unchecked")
        final ModifiableRelation<Object> relation = (ModifiableRelation<Object>) targets[i];
        relation.insert(var, objpackages.data(j, i));
      }
      newids.add(var);
    }

    // fire insertion event
    eventManager.fireObjectsInserted(newids);
    return newids;
  }

  /**
   * Find a mapping from package columns to database columns, eventually adding
   * new database columns when needed.
   *
   * @param pack Package to process
   * @return Column mapping
   */
  protected Relation<?>[] alignColumns(ObjectBundle pack) {
    // align representations.
    Relation<?>[] targets = new Relation<?>[pack.metaLength()];
    long[] used = BitsUtil.zero(relations.size());
    for(int i = 0; i < targets.length; i++) {
      SimpleTypeInformation<?> meta = pack.meta(i);
      // TODO: aggressively try to match exact metas first?
      // Try to match unused representations only
      for(int j = BitsUtil.nextClearBit(used, 0); j >= 0 && j < relations.size(); j = BitsUtil.nextClearBit(used, j + 1)) {
        Relation<?> relation = relations.get(j);
        if(relation.getDataTypeInformation().isAssignableFromType(meta)) {
          targets[i] = relation;
          BitsUtil.setI(used, j);
          break;
        }
      }
      if(targets[i] == null) {
        targets[i] = addNewRelation(meta);
        BitsUtil.setI(used, relations.size() - 1);
      }
    }
    return targets;
  }

  /**
   * Add a new representation for the given meta.
   *
   * @param meta meta data
   * @return new representation
   */
  private Relation<?> addNewRelation(SimpleTypeInformation<?> meta) {
    @SuppressWarnings("unchecked")
    SimpleTypeInformation<Object> ometa = (SimpleTypeInformation<Object>) meta;
    Relation<?> relation = new MaterializedRelation<>(ometa, ids);
    relations.add(relation);
    getHierarchy().add(this, relation);
    // Try to add indexes where appropriate
    for(IndexFactory<?> factory : indexFactories) {
      if(factory.getInputTypeRestriction().isAssignableFromType(meta)) {
        @SuppressWarnings("unchecked")
        final IndexFactory<Object> ofact = (IndexFactory<Object>) factory;
        @SuppressWarnings("unchecked")
        final Relation<Object> orep = (Relation<Object>) relation;
        Index index = ofact.instantiate(orep);
        index.initialize();
        getHierarchy().add(relation, index);
      }
    }
    return relation;
  }

  /**
   * Removes the objects from the database (by calling
   * {@link #doDelete(DBIDRef)} for each object) and indexes and fires a
   * deletion event.
   *
   * {@inheritDoc}
   */
  @Override
  public MultipleObjectsBundle delete(DBIDs ids) {
    // Prepare bundle to return
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    for(Relation<?> relation : relations) {
      ArrayList<Object> data = new ArrayList<>(ids.size());
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        data.add(relation.get(iter));
      }
      bundle.appendColumn(relation.getDataTypeInformation(), data);
    }
    // remove from db
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      doDelete(iter);
    }
    // fire deletion event
    eventManager.fireObjectsRemoved(ids);

    return bundle;
  }

  /**
   * Removes the object from the database (by calling {@link #doDelete(DBIDRef)}
   * ) and indexes and fires a deletion event.
   *
   * {@inheritDoc}
   */
  @Override
  public SingleObjectBundle delete(DBIDRef id) {
    // Prepare bundle to return
    SingleObjectBundle bundle = new SingleObjectBundle();
    for(Relation<?> relation : relations) {
      bundle.append(relation.getDataTypeInformation(), relation.get(id));
    }
    doDelete(id);
    // fire deletion event
    eventManager.fireObjectRemoved(id);

    return bundle;
  }

  /**
   * Removes the object with the specified id from this database.
   *
   * @param id id the id of the object to be removed
   */
  private void doDelete(DBIDRef id) {
    // Remove id
    ids.remove(id);
    // Remove from all representations.
    for(Relation<?> relation : relations) {
      // ID has already been removed, and this would loop...
      if(relation == idrep) {
        continue;
      }
      if(!(relation instanceof ModifiableRelation)) {
        throw new AbortException("Non-modifiable relations have been added to the database.");
      }
      ((ModifiableRelation<?>) relation).delete(id);
    }
    DBIDFactory.FACTORY.deallocateSingleDBID(id);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDatabase.Parameterizer {
    /**
     * Holds the database connection to get the initial data from.
     */
    protected DatabaseConnection databaseConnection = null;

    /**
     * Indexes to add.
     */
    private Collection<? extends IndexFactory<?>> indexFactories;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Get database connection.
      final ObjectParameter<DatabaseConnection> dbcP = new ObjectParameter<>(DATABASE_CONNECTION_ID, DatabaseConnection.class, FileBasedDatabaseConnection.class);
      if(config.grab(dbcP)) {
        databaseConnection = dbcP.instantiateClass(config);
      }
      // Get indexes.
      final ObjectListParameter<IndexFactory<?>> indexFactoryP = new ObjectListParameter<>(INDEX_ID, IndexFactory.class, true);
      if(config.grab(indexFactoryP)) {
        indexFactories = indexFactoryP.instantiateClasses(config);
      }
    }

    @Override
    protected HashmapDatabase makeInstance() {
      return new HashmapDatabase(databaseConnection, indexFactories);
    }
  }
}
