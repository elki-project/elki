package de.lmu.ifi.dbs.varianceanalysis;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * Computes the principal components for vector objects of a given database.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class GlobalPCA extends AbstractPCA {
  /**
   * Holds the covariance matrix.
   */
  private Matrix covarianceMatrix;

  /**
   * Computes the principal components for objects of the given database.
   *
   * @param database the database containing the objects
   */
  public void run(Database<RealVector> database) {
    covarianceMatrix = Util.covarianceMatrix(database);
    if (debug) {
      debugFine("covarianceMatrix " + covarianceMatrix);
    }

    determineEigenPairs(covarianceMatrix);
  }

  /**
   * Computes the principal components for objects of the given matrix.
   *
   * @param matrix the database containing the objects
   */
  public void run(Matrix matrix) {
    covarianceMatrix = Util.covarianceMatrix(matrix);
    if (debug) {
      debugFine("covarianceMatrix " + covarianceMatrix);
    }

    determineEigenPairs(covarianceMatrix);
  }

  /**
   * Returns the covariance matrix.
   *
   * @return the covariance matrix
   */
  public Matrix getCovarianceMatrix() {
    return covarianceMatrix;
  }
}
