package de.lmu.ifi.dbs.elki.database;

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

import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.StaticDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.DBIDView;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.bundle.ObjectBundle;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
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
 * 
 * @apiviz.landmark
 * @apiviz.composedOf ArrayDBIDs
 * @apiviz.uses DatabaseConnection
 */
@Description("Database using an in-memory hashtable and at least providing linear scans.")
public class StaticArrayDatabase extends AbstractDatabase implements Database, Parameterizable {
  /**
   * Our logger
   */
  private static final Logging logger = Logging.getLogger(StaticArrayDatabase.class);

  /**
   * IDs of this database
   */
  private ArrayDBIDs ids;

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
  public StaticArrayDatabase(DatabaseConnection databaseConnection, Collection<IndexFactory<?, ?>> indexFactories) {
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
   */
  public StaticArrayDatabase() {
    this(null, null);
  }

  /**
   * Initialize the database by getting the initial data from the database
   * connection.
   */
  @Override
  public void initialize() {
    if(databaseConnection != null) {
      if(logger.isDebugging()) {
        logger.debugFine("Loading data from database connection.");
      }
      MultipleObjectsBundle objpackages = databaseConnection.loadData();
      // Run at most once.
      databaseConnection = null;

      // Find DBID column
      int idrepnr = findDBIDColumn(objpackages);
      // Build DBID array
      if(idrepnr == -1) {
        this.ids = DBIDUtil.generateStaticDBIDRange(objpackages.dataLength());
      }
      else {
        final ArrayModifiableDBIDs newids = DBIDUtil.newArray(objpackages.dataLength());
        for(int j = 0; j < objpackages.dataLength(); j++) {
          DBID newid = (DBID) objpackages.data(j, idrepnr);
          newids.add(newid);
        }
        this.ids = newids;
      }
      // Replace id representation.
      // TODO: this is an ugly hack
      this.idrep = new DBIDView(this, this.ids);
      relations.add(this.idrep);
      getHierarchy().add(this, idrep);

      // insert into db - note: DBIDs should have been prepared before this!
      Relation<?>[] targets = alignColumns(objpackages);

      for(int j = 0; j < objpackages.dataLength(); j++) {
        // insert object
        final DBID newid = ids.get(j);
        for(int i = 0; i < targets.length; i++) {
          // DBIDs were handled above.
          if(i == idrepnr) {
            continue;
          }
          @SuppressWarnings("unchecked")
          final Relation<Object> relation = (Relation<Object>) targets[i];
          relation.set(newid, objpackages.data(j, i));
        }
      }

      for(Relation<?> relation : relations) {
        SimpleTypeInformation<?> meta = relation.getDataTypeInformation();
        // Try to add indexes where appropriate
        for(IndexFactory<?, ?> factory : indexFactories) {
          if(factory.getInputTypeRestriction().isAssignableFromType(meta)) {
            @SuppressWarnings("unchecked")
            final IndexFactory<Object, ?> ofact = (IndexFactory<Object, ?>) factory;
            @SuppressWarnings("unchecked")
            final Relation<Object> orep = (Relation<Object>) relation;
            final Index index = ofact.instantiate(orep);
            addIndex(index);
            index.insertAll(ids);
          }
        }
      }

      // fire insertion event
      eventManager.fireObjectsInserted(ids);
    }
  }

  @Override
  public void addIndex(Index index) {
    this.indexes.add(index);
    // TODO: actually add index to the representation used?
    this.addChildResult(index);
  }

  @Override
  public Collection<Index> getIndexes() {
    return Collections.unmodifiableList(this.indexes);
  }

  @Override
  public void removeIndex(Index index) {
    this.indexes.remove(index);
    this.getHierarchy().remove(this, index);
  }

  /**
   * Find an DBID column.
   * 
   * @param pack Package to process
   * @return DBID column
   */
  protected int findDBIDColumn(ObjectBundle pack) {
    for(int i = 0; i < pack.metaLength(); i++) {
      SimpleTypeInformation<?> meta = pack.meta(i);
      if(TypeUtil.DBID.isAssignableFromType(meta)) {
        return i;
      }
    }
    return -1;
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
    BitSet used = new BitSet(relations.size());
    for(int i = 0; i < targets.length; i++) {
      SimpleTypeInformation<?> meta = pack.meta(i);
      // TODO: aggressively try to match exact metas first?
      // Try to match unused representations only
      for(int j = used.nextClearBit(0); j >= 0 && j < relations.size(); j = used.nextClearBit(j + 1)) {
        Relation<?> relation = relations.get(j);
        if(relation.getDataTypeInformation().isAssignableFromType(meta)) {
          targets[i] = relation;
          used.set(j);
          break;
        }
      }
      if(targets[i] == null) {
        targets[i] = addNewRelation(meta);
        used.set(relations.size() - 1);
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
    Relation<?> relation = new MaterializedRelation<Object>(this, ometa, ids);
    relations.add(relation);
    getHierarchy().add(this, relation);
    return relation;
  }

  @Deprecated
  @Override
  public final int size() {
    return ids.size();
  }

  @Deprecated
  @Override
  public StaticDBIDs getDBIDs() {
    return DBIDUtil.makeUnmodifiable(ids);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Holds the database connection to get the initial data from.
     */
    protected DatabaseConnection databaseConnection = null;

    /**
     * Indexes to add.
     */
    private Collection<IndexFactory<?, ?>> indexFactories;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Get database connection.
      final ObjectParameter<DatabaseConnection> dbcP = new ObjectParameter<DatabaseConnection>(OptionID.DATABASE_CONNECTION, DatabaseConnection.class, FileBasedDatabaseConnection.class);
      if(config.grab(dbcP)) {
        databaseConnection = dbcP.instantiateClass(config);
      }
      // Get indexes.
      final ObjectListParameter<IndexFactory<?, ?>> indexFactoryP = new ObjectListParameter<IndexFactory<?, ?>>(INDEX_ID, IndexFactory.class, true);
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