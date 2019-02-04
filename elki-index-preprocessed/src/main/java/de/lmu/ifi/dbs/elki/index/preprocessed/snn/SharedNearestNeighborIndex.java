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
package de.lmu.ifi.dbs.elki.index.preprocessed.snn;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;

/**
 * Interface for an index providing nearest neighbor sets.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public interface SharedNearestNeighborIndex<O> extends Index {
  /**
   * Get the precomputed nearest neighbors
   *
   * @param id Object ID
   * @return Neighbor DBIDs
   */
  ArrayDBIDs getNearestNeighborSet(DBIDRef id);

  /**
   * Get the number of neighbors
   *
   * @return NN size
   */
  int getNumberOfNeighbors();

  /**
   * Factory interface
   *
   * @author Erich Schubert
   *
   * @stereotype factory
   * @navassoc - create - SharedNearestNeighborIndex
   *
   * @param <O> The input object type
   */
  interface Factory<O> extends IndexFactory<O> {
    /**
     * Instantiate the index for a given database.
     *
     * @param database Database type
     *
     * @return Index
     */
    @Override
    SharedNearestNeighborIndex<O> instantiate(Relation<O> database);

    /**
     * Get the number of neighbors
     *
     * @return NN size
     */
    int getNumberOfNeighbors();
  }
}
