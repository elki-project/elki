package de.lmu.ifi.dbs.varianceanalysis;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.List;

/**
 * Performs a linear local PCA based on the covariance matrices of given
 * objects.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LinearLocalPCA extends AbstractLocalPCA {
  /**
   * Performs the actual eigenvalue decomposition on the specified object ids
   * stored in the given database.
   *
   * @param database the database holding the objects
   * @param ids      the list of the object ids for which the eigenvalue
   *                 decomposition should be performed
   * @return the actual eigenvalue decomposition on the specified object ids
   *         stored in the given database
   */
  protected EigenvalueDecomposition eigenValueDecomposition(
  Database<RealVector> database, List<Integer> ids) {
    StringBuffer msg = new StringBuffer();

    // covariance matrix
    Matrix covariance = Util.covarianceMatrix(database, ids);

    if (DEBUG) {
      msg.append("\ncov ");
      msg.append(covariance);
    }

    EigenvalueDecomposition evd = covariance.eig();

    // correlation matrixArray
    // double[][] cov = covariance.getArray();
    // double[][] corr = new double[cov.length][];
    // for (int i=0; i<cov.length; i++) {
    // corr[i] = new double[cov[i].length];
    // for (int j=0; j<cov[i].length; j++) {
    // corr[i][j] = cov[i][j] / Math.sqrt(cov[i][i] * cov [j][j]);
    // }
    // }
    // Matrix correlationMatrix = new Matrix(corr);
    // EigenvalueDecomposition evd = correlationMatrix.eig();

    if (DEBUG) {
      logger.info(msg.toString());
    }

    return evd;
  }
}
