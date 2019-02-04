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
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;

/**
 * Class for building a "traditional" covariance matrix vai
 * {@link CovarianceMatrix}.
 * Reasonable default choice for a {@link CovarianceMatrixBuilder}
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @assoc - - - CovarianceMatrix
 */
public class StandardCovarianceMatrixBuilder implements CovarianceMatrixBuilder {
  /**
   * Compute Covariance Matrix for a collection of database IDs.
   * 
   * @param ids a collection of ids
   * @param database the database used
   * @return Covariance Matrix
   */
  @Override
  public double[][] processIds(DBIDs ids, Relation<? extends NumberVector> database) {
    return CovarianceMatrix.make(database, ids).destroyToPopulationMatrix();
  }
}
