package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;

/**
 * Interface for computing covariance matrixes on a data set.
 * 
 * @author Erich Schubert
 *
 * @param <V> Vector base type
 */
public interface CovarianceMatrixBuilder<V extends NumberVector<?>> {
  /**
   * Compute Covariance Matrix for a complete database.
   * 
   * @param database the database used
   * @return Covariance Matrix
   */
  Matrix processDatabase(Relation<? extends V> database);

  /**
   * Compute Covariance Matrix for a collection of database IDs.
   * 
   * @param ids a collection of ids
   * @param database the database used
   * @return Covariance Matrix
   */
  Matrix processIds(DBIDs ids, Relation<? extends V> database);

  /**
   * Compute Covariance Matrix for a QueryResult Collection.
   * 
   * By default it will just collect the ids and run processIds
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @param k the number of entries to process
   * @param <D> distance type
   * @return Covariance Matrix
   */
  <D extends NumberDistance<D, ?>> Matrix processQueryResults(DistanceDBIDResult<D> results, Relation<? extends V> database, int k);

  /**
   * Compute Covariance Matrix for a QueryResult Collection.
   * 
   * By default it will just collect the ids and run processIds
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @param <D> distance type
   * @return Covariance Matrix
   */
  <D extends NumberDistance<D, ?>> Matrix processQueryResults(DistanceDBIDResult<D> results, Relation<? extends V> database);
}