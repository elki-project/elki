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
package elki.outlier.spatial.neighborhood;

import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;

/**
 * Predicate to obtain the neighbors of a reference object as set.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public interface NeighborSetPredicate {
  /**
   * Get the neighbors of a reference object for DBSCAN.
   * 
   * @param reference Reference object
   * @return Neighborhood
   */
  DBIDs getNeighborDBIDs(DBIDRef reference);

  /**
   * Factory interface to produce instances.
   * 
   * @author Erich Schubert
   *
   * @stereotype factory
   * @has - - - NeighborSetPredicate
   * 
   * @param <O> Input relation object type restriction
   */
  interface Factory<O> {
    /**
     * Instantiation method.
     * 
     * @param database Database
     * @param relation Relation to instantiate for.
     * @return instance
     */
    NeighborSetPredicate instantiate(Database database, Relation<? extends O> relation);

    /**
     * Get the input type information
     * 
     * @return input type
     */
    TypeInformation getInputTypeRestriction();
  }
}