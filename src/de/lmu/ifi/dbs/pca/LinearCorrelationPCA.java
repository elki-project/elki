package de.lmu.ifi.dbs.pca;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.List;

/**
 * Performs a linear PCA based on the covariance matrices of given objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LinearCorrelationPCA extends AbstractCorrelationPCA {

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

    // centroid
    DoubleVector centroid = Util.centroid(database, ids);
    msg.append("\ncentroid ");
    msg.append(centroid);

    // covariance matrixArray
    int columns = centroid.getDimensionality();
    int rows = ids.size();
    double[][] matrixArray = new double[rows][columns];

    for (int i = 0; i < rows; i++) {
      DoubleVector obj = database.get(ids.get(i));
      for (int d = 0; d < columns; d++) {
        matrixArray[i][d] = obj.getValue(d + 1) - centroid.getValue(d + 1);
      }
    }
    Matrix centeredMatrix = new Matrix(matrixArray);
    Matrix covariance = centeredMatrix.transpose().times(centeredMatrix);
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
