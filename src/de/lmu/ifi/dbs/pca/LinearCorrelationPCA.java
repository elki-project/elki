package de.lmu.ifi.dbs.pca;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.List;

/**
 * Performs a linear PCA based on the covariance matrices of given objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LinearCorrelationPCA extends AbstractCorrelationPCA {

  /**
   * Performs the actual eigenvalue decomposition on the specified objects
   * stored in the given database.
   *
   * @param database the database holding the objects
   * @param objects  the lis of the objects foer which the eigenvalue decomposition
   *                 should be performed
   * @return the actual eigenvalue decomposition on the specified objects
   *         stored in the given database
   */
  protected EigenvalueDecomposition eigenValueDecomposition(Database database,
                                                            List<QueryResult> objects) {
    StringBuffer msg = new StringBuffer();

    // centroid
    int dim = ((RealVector) database.get(objects.get(0).getID())).getDimensionality();
    double[] centroid = new double[dim];

    for (QueryResult object : objects) {
      RealVector o = (RealVector) database.get(object.getID());
      for (int j = 1; j <= dim; j++) {
        centroid[j-1] += o.getValue(j);
      }
    }
    for (int i = 0; i < dim; i++) {
      centroid[i] /= objects.size();
    }
    msg.append("\ncentroid ");
    msg.append(Util.format(centroid, ",", 4));

    // covariance matrixArray
    int columns = centroid.length;
    int rows = objects.size();
    double[][] matrixArray = new double[rows][columns];

    for (int i = 0; i < rows; i++) {
      int id = objects.get(i).getID();
      RealVector obj = (RealVector) database.get(id);
      for (int d = 0; d < columns; d++) {
        matrixArray[i][d] = obj.getValue(d+1) - centroid[d];
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
