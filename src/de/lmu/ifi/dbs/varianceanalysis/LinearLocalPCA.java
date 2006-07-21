package de.lmu.ifi.dbs.varianceanalysis;

import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * Performs a linear local PCA based on the covariance matrices of given
 * objects.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LinearLocalPCA extends LocalPCA {

  /**
   * @see LocalPCA#sortedEigenPairs(de.lmu.ifi.dbs.database.Database, java.util.List)
   */
  protected SortedEigenPairs sortedEigenPairs(Database<RealVector> database, List<Integer> ids) {

    // covariance matrix
    Matrix covariance = Util.covarianceMatrix(database, ids);
    // eigen value decomposition
    EigenvalueDecomposition evd = covariance.eig();
    SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, false);

    if (this.debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\ncov ").append(covariance);
      msg.append("\neigenpairs: ").append(Arrays.asList(eigenPairs));
      debugFine(msg.toString());
    }

    return eigenPairs;
  }
}
