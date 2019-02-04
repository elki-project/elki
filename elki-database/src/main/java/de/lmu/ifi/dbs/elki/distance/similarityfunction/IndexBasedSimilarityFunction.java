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
package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.database.query.similarity.DatabaseSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.Index;

/**
 * Interface for preprocessor/index based similarity functions.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @navhas - create - Instance
 * @opt nodefillcolor LemonChiffon
 *
 * @param <O> Object type
 */
public interface IndexBasedSimilarityFunction<O> extends SimilarityFunction<O> {
  /**
   * Preprocess the database to get the actual distance function.
   * 
   * @param database
   * @return Actual distance query.
   */
  @Override
  <T extends O> Instance<T, ?> instantiate(Relation<T> database);

  /**
   * Instance interface for index/preprocessor based distance functions.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Object type
   */
  interface Instance<T, I extends Index> extends DatabaseSimilarityQuery<T> {
    /**
     * Get the index used.
     * 
     * @return the index used
     */
    I getIndex();
  }
}
