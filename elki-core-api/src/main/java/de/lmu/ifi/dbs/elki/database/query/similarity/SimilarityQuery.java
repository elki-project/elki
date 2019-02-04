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
package de.lmu.ifi.dbs.elki.database.query.similarity;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;

/**
 * A similarity query serves as adapter layer for database and primitive
 * similarity functions.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @opt nodefillcolor LemonChiffon
 * 
 * @param <O> Input object type
 */
public interface SimilarityQuery<O> extends DatabaseQuery {
  /**
   * Returns the similarity between the two objects specified by their object
   * ids.
   * 
   * @param id1 first object id
   * @param id2 second object id
   * @return the similarity between the two objects specified by their object
   *         ids
   */
  double similarity(DBIDRef id1, DBIDRef id2);

  /**
   * Returns the similarity between the two objects specified by their object
   * ids.
   * 
   * @param o1 first object
   * @param id2 second object id
   * @return the similarity between the two objects specified by their object
   *         ids
   */
  double similarity(O o1, DBIDRef id2);

  /**
   * Returns the similarity between the two objects specified by their object
   * ids.
   * 
   * @param id1 first object id
   * @param o2 second object
   * @return the similarity between the two objects specified by their object
   *         ids
   */
  double similarity(DBIDRef id1, O o2);

  /**
   * Returns the similarity between the two objects specified by their object
   * ids.
   * 
   * @param o1 first object
   * @param o2 second object
   * @return the similarity between the two objects specified by their object
   *         ids
   */
  double similarity(O o1, O o2);

  /**
   * Access the underlying data query.
   * 
   * @return data query in use
   */
  Relation<? extends O> getRelation();

  /**
   * Get the inner similarity function.
   * 
   * @return Similarity function
   */
  SimilarityFunction<? super O> getSimilarityFunction();
}