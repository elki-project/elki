package experimentalcode.shared.parallelcoord;

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
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;

/**
 * Class to compute the dimension similarity based on covariances.
 * 
 * @author Erich Schubert
 */
public class CovarianceDimensionSimilarity implements DimensionSimilarity<NumberVector<?>> {
  @Override
  public double[][] computeDimensionSimilarites(Relation<? extends NumberVector<?>> relation, DBIDs subset) {
    final int dim = RelationUtil.dimensionality(relation);
    CovarianceMatrix covmat = CovarianceMatrix.make(relation, subset);
    double[][] mat = covmat.destroyToSampleMatrix().getArrayRef();
    // Transform diagonal to 1 / stddev
    for (int i = 0; i < dim; i++) {
      mat[i][i] = 1. / Math.sqrt(mat[i][i]);
    }
    // Rescale others using the expected covariance
    for (int i = 0; i < dim; i++) {
      for (int j = i + 1; j < dim; j++) {
        mat[i][j] = mat[i][j] * mat[i][i] * mat[j][j];
        mat[j][i] = mat[i][j];
      }
    }
    // To avoid confusion, zero out the diagonal.
    for (int i = 0; i < dim; i++) {
      mat[i][i] = 0;
    }
    return mat;
  }
}
