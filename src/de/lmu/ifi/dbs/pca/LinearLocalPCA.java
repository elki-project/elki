package de.lmu.ifi.dbs.pca;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.List;

/**
 * Performs a linear local PCA based on the covariance matrices of given objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LinearLocalPCA extends AbstractLocalPCA {

  /**
   * Provides a new PCA that cperforms a linear PCA based on the covarince matrices
   * of given objects of a certain database. Since LinearCorrelationPCA is a non-abstract
   * class, finally optionHandler is initialized.
   */
  public LinearLocalPCA() {
    super();
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * Performs the actual eigenvalue decomposition on the specified object ids
   * stored in the given database.
   *
   * @param database the database holding the objects
   * @param ids      the list of the object ids for which the eigenvalue decomposition
   *                 should be performed
   * @return the actual eigenvalue decomposition on the specified object ids
   *         stored in the given database
   */
  protected EigenvalueDecomposition eigenValueDecomposition(Database<DoubleVector> database,
                                                            List<Integer> ids) {
    StringBuffer msg = new StringBuffer();

    // covariance matrix
    Matrix covariance = Util.covarianceMatrix(database, ids);
    msg.append("\ncov ");
    msg.append(covariance);

    EigenvalueDecomposition evd = covariance.eig();

    // correlation matrixArray
//    double[][] cov = covariance.getArray();
//    double[][] corr = new double[cov.length][];
//    for (int i=0; i<cov.length; i++) {
//      corr[i] = new double[cov[i].length];
//      for (int j=0; j<cov[i].length; j++) {
//        corr[i][j] = cov[i][j] / Math.sqrt(cov[i][i] * cov [j][j]);
//      }
//    }
//    Matrix correlationMatrix = new Matrix(corr);
//    EigenvalueDecomposition evd = correlationMatrix.eig();

    logger.info(msg.toString());
    return evd;
  }
}
