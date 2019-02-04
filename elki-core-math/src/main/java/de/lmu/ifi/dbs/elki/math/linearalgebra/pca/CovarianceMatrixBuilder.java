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
package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.relation.Relation;

/**
 * Interface for computing covariance matrixes on a data set.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public interface CovarianceMatrixBuilder {
  /**
   * Compute Covariance Matrix for a complete relation.
   * 
   * @param relation the relation to run on
   * @return Covariance Matrix
   */
  default double[][] processRelation(Relation<? extends NumberVector> relation) {
    return processIds(relation.getDBIDs(), relation);
  }

  /**
   * Compute Covariance Matrix for a collection of database IDs.
   * 
   * @param ids a collection of ids
   * @param database the database used
   * @return Covariance Matrix
   */
  double[][] processIds(DBIDs ids, Relation<? extends NumberVector> database);

  /**
   * Compute Covariance Matrix for a QueryResult Collection.
   * 
   * By default it will just run processIds, but subclasses <em>may</em> use the
   * distances for weighting.
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @param k the number of entries to process
   * @return Covariance Matrix
   */
  default double[][] processQueryResults(DoubleDBIDList results, Relation<? extends NumberVector> database, int k) {
    if(results.size() > k) {
      ModifiableDBIDs ids = DBIDUtil.newArray(k);
      for(DBIDIter it = results.iter(); it.valid() && ids.size() < k; it.advance()) {
        ids.add(it);
      }
      return processIds(ids, database);
    }
    return processIds(results, database);
  }

  /**
   * Compute Covariance Matrix for a QueryResult Collection.
   * 
   * By default it will just collect the ids and run processIds, but subclasses
   * <em>may</em> use the distance for weighting.
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @return Covariance Matrix
   */
  default double[][] processQueryResults(DoubleDBIDList results, Relation<? extends NumberVector> database) {
    return processQueryResults(results, database, results.size());
  }
}
