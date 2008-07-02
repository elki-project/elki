package de.lmu.ifi.dbs.elki.varianceanalysis;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.Util;

/**
 * Computes the principal components for vector objects of a given database.
 *
 * @author Elke Achtert 
 */
public class GlobalPCA<O extends RealVector<O, ? >> extends AbstractPCA {
  /**
   * Holds the covariance matrix.
   */
  private Matrix covarianceMatrix;

  /**
   * Computes the principal components for vector objects of a given database.
   */
  public GlobalPCA() {
    super();
//    this.debug = true;
  }

  /**
   * Computes the principal components for objects of the given database.
   *
   * @param database the database containing the objects
   */
  public void run(Database<O> database) {
    covarianceMatrix = Util.covarianceMatrix(database);
    if (debug) {
      debugFine("covarianceMatrix " + covarianceMatrix.dimensionInfo() +
                "\n" + covarianceMatrix);
    }

    determineEigenPairs(covarianceMatrix);
  }

  /**
   * Computes the principal components for objects of the given matrix.
   *
   * @param matrix the matrix containing the objects as column vectors
   */
  public void run(Matrix matrix) {
    covarianceMatrix = Util.covarianceMatrix(matrix);
    if (debug) {
      debugFine("covarianceMatrix " + covarianceMatrix.dimensionInfo()
                + "\n" + covarianceMatrix);
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
