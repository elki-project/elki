package de.lmu.ifi.dbs.elki.math.dimensionsimilarity;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Class to compute the dimension similarity based on covariances.
 *
 * @author Erich Schubert
 * @since 0.5.5
 */
public class CovarianceDimensionSimilarity implements DimensionSimilarity<NumberVector> {
  /**
   * Static instance
   */
  public static final CovarianceDimensionSimilarity STATIC = new CovarianceDimensionSimilarity();

  /**
   * Constructor. Use static instance.
   */
  protected CovarianceDimensionSimilarity() {
    super();
  }

  @Override
  public void computeDimensionSimilarites(Relation<? extends NumberVector> relation, DBIDs subset, DimensionSimilarityMatrix matrix) {
    final int dim = matrix.size();
    // FIXME: Use only necessary dimensions!
    CovarianceMatrix covmat = CovarianceMatrix.make(relation, subset);
    double[][] mat = covmat.destroyToSampleMatrix().getArrayRef();
    // Transform diagonal to 1 / stddev
    for (int i = 0; i < mat.length; i++) {
      mat[i][i] = 1. / Math.sqrt(mat[i][i]);
    }
    // Fill output matrix:
    for (int x = 0; x < dim; x++) {
      final int i = matrix.dim(x);
      for (int y = x + 1; y < dim; y++) {
        final int j = matrix.dim(y);
        matrix.set(x, y, mat[i][j] * mat[i][i] * mat[j][j]);
      }
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected CovarianceDimensionSimilarity makeInstance() {
      return STATIC;
    }
  }
}
