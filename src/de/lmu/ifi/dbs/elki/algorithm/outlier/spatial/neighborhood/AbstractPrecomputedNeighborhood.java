package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood;
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

import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Abstract base class for precomputed neighborhoods.
 * 
 * @author Erich Schubert
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
  public DBIDs getNeighborDBIDs(DBID reference) {
    DBIDs neighbors = store.get(reference);
    if(neighbors != null) {
      return neighbors;
    }
    else {
      // Use just the object itself.
      if(getLogger().isDebugging()) {
        getLogger().warning("No neighbors for object " + reference);
      }
      return reference;
    }
  }

  /**
   * The logger to use for error reporting.
   * 
   * @return Logger
   */
  abstract protected Logging getLogger();

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.has AbstractPrecomputedNeighborhood
   */
  public abstract static class Factory<O> implements NeighborSetPredicate.Factory<O> {
    // Nothing to add.
  }
}