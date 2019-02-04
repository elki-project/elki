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

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayStaticDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.DBIDView;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * This database class uses array-based storage and thus does not allow for
 * dynamic insert, delete and update operations. However, array access is
 * expected to be faster and use less memory.
 *
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @opt nodefillcolor LemonChiffon
 * @composed - - - ArrayStaticDBIDs
 * @assoc - - - DatabaseConnection
 */
@Description("Database using an in-memory hashtable and at least providing linear scans.")
public class StaticArrayDatabase extends AbstractDatabase {
  /**
   * Our logger
   */
  private static final Logging LOG = Logging.getLogger(StaticArrayDatabase.class);

  /**
   * IDs of this database
   */
  private ArrayStaticDBIDs ids;

  /**
   * The DBID representation we use
   */
  private DBIDView idrep;

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
  public StaticArrayDatabase(DatabaseConnection databaseConnection, Collection<? extends IndexFactory<?>> indexFactories) {
    super();
    this.databaseConnection = databaseConnection;
    this.ids = null;
    this.idrep = null;

    // Add indexes.
    if(indexFactories != null) {
      this.indexFactories.addAll(indexFactories);
    }
  }

  /**
   * Constructor with no indexes.
   *
   * @param databaseConnection Database connection to get the initial data from.
   */
  public StaticArrayDatabase(DatabaseConnection databaseConnection) {
    this(databaseConnection, null);
  }

  /**
   * Initialize the database by getting the initial data from the database
   * connection.
   */
  @Override
  public void initialize() {
    if(databaseConnection == null) {
      return; // Supposedly we initialized already.
    }
    if(LOG.isDebugging()) {
      LOG.debugFine("Loading data from database connection.");
    }
    MultipleObjectsBundle bundle = databaseConnection.loadData();
    // Run at most once.
    databaseConnection = null;

    // Find DBIDs for bundle
    {
      DBIDs bids = bundle.getDBIDs();
      if(bids instanceof ArrayStaticDBIDs) {
        this.ids = (ArrayStaticDBIDs) bids;
      }
      else if(bids == null) {
        this.ids = DBIDUtil.generateStaticDBIDRange(bundle.dataLength());
      }
      else {
        this.ids = (ArrayStaticDBIDs) DBIDUtil.makeUnmodifiable(DBIDUtil.ensureArray(bids));
      }
    }
    // Replace id representation (it would be nicer if we would not need
    // DBIDView at all)
    this.idrep = new DBIDView(this.ids);
    relations.add(this.idrep);
    getHierarchy().add(this, idrep);

    DBIDArrayIter it = this.ids.iter();

    int numrel = bundle.metaLength();
    for(int i = 0; i < numrel; i++) {
      SimpleTypeInformation<?> meta = bundle.meta(i);
      @SuppressWarnings("unchecked")
      SimpleTypeInformation<Object> ometa = (SimpleTypeInformation<Object>) meta;
      WritableDataStore<Object> store = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, ometa.getRestrictionClass());
      for(it.seek(0); it.valid(); it.advance()) {
        store.put(it, bundle.data(it.getOffset(), i));
      }
      Relation<?> relation = new MaterializedRelation<>(ometa, ids, null, store);
      relations.add(relation);
      getHierarchy().add(this, relation);

      // Try to add indexes where appropriate
      for(IndexFactory<?> factory : indexFactories) {
        if(factory.getInputTypeRestriction().isAssignableFromType(ometa)) {
          @SuppressWarnings("unchecked")
          final IndexFactory<Object> ofact = (IndexFactory<Object>) factory;
          @SuppressWarnings("unchecked")
          final Relation<Object> orep = (Relation<Object>) relation;
          final Index index = ofact.instantiate(orep);
          Duration duration = LOG.isStatistics() ? LOG.newDuration(index.getClass().getName() + ".construction").begin() : null;
          index.initialize();
          if(duration != null) {
            LOG.statistics(duration.end());
          }
          getHierarchy().add(relation, index);
        }
      }
    }

    // fire insertion event
    eventManager.fireObjectsInserted(ids);
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
    protected StaticArrayDatabase makeInstance() {
      return new StaticArrayDatabase(databaseConnection, indexFactories);
    }
  }
}
