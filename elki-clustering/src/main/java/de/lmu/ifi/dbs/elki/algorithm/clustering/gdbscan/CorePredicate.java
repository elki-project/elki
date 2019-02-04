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
package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * Predicate for GeneralizedDBSCAN to evaluate whether a point is a core point
 * or not.
 * 
 * Note the Factory/Instance split of this interface.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @has - - - Instance
 * 
 * @param <T> Data type of neighborhoods
 */
public interface CorePredicate<T> {
  /**
   * Instantiate for a database.
   * 
   * @param database Database to instantiate for
   * @return Instance
   */
  Instance<? super T> instantiate(Database database);

  /**
   * Test whether the neighborhood type T is accepted by this predicate.
   * 
   * @param type Type information
   * @return true when the type is accepted
   */
  boolean acceptsType(SimpleTypeInformation<? extends T> type);
  
  /**
   * Instance for a particular data set.
   * 
   * @author Erich Schubert
   * 
   * @param <T> actual type
   */
  interface Instance<T> {
    /**
     * Decide whether the point is a core point, based on its neighborhood.
     * 
     * @param point Query point
     * @param neighbors Neighbors
     * @return core point property
     */
    boolean isCorePoint(DBIDRef point, T neighbors);
  }
}