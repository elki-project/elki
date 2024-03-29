/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.clustering.dbscan.predicates;

import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;

/**
 * Get the neighbors of an object
 * <p>
 * Note the Factory/Instance split of this interface.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @has - - - Instance
 * 
 * @param <O> Input object type
 * @param <T> Data type of neighborhoods
 */
public interface NeighborPredicate<O, T> {
  /**
   * Instantiate for a database.
   * 
   * @param database Database to instantiate for
   * @return Instance
   */
  Instance<T> instantiate(Relation<? extends O> database);

  /**
   * Input data type restriction.
   * 
   * @return Type restriction
   */
  TypeInformation getInputTypeRestriction();

  /**
   * Output data type information.
   * 
   * @return Type information
   */
  SimpleTypeInformation<T> getOutputType();

  /**
   * Instance for a particular data set.
   * 
   * @author Erich Schubert
   */
  interface Instance<T> {
    /**
     * Get the neighbors of a reference object for DBSCAN.
     * 
     * @param reference Reference object
     * @return Neighborhood
     */
    T getNeighbors(DBIDRef reference);

    /**
     * Get the IDs the predicate is defined for.
     * 
     * @return Database ids
     */
    DBIDs getIDs();

    /**
     * Add the neighbors to a DBID set
     * 
     * @param neighbors Neighbors to iterate over
     * @return iterator
     */
    DBIDIter iterDBIDs(T neighbors);
  }
}
