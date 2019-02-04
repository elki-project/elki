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
package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood;

import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Abstract base class for precomputed neighborhoods.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public abstract class AbstractPrecomputedNeighborhood implements NeighborSetPredicate {
  /**
   * The data
   */
  protected DataStore<DBIDs> store;
  
  /**
   * Constructor.
   *
   * @param store the actual data.
   */
  public AbstractPrecomputedNeighborhood(DataStore<DBIDs> store) {
    super();
    this.store = store;
  }

  @Override
  public DBIDs getNeighborDBIDs(DBIDRef reference) {
    DBIDs neighbors = store.get(reference);
    if(neighbors != null) {
      return neighbors;
    }
    else {
      // Use just the object itself.
      if(getLogger().isDebugging()) {
        getLogger().warning("No neighbors for object " + reference);
      }
      return DBIDUtil.deref(reference);
    }
  }

  /**
   * The logger to use for error reporting.
   * 
   * @return Logger
   */
  protected abstract Logging getLogger();

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   * 
   * @stereotype factory
   * @has - - - AbstractPrecomputedNeighborhood
   */
  public abstract static class Factory<O> implements NeighborSetPredicate.Factory<O> {
    // Nothing to add.
  }
}