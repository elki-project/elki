package de.lmu.ifi.dbs.varianceanalysis;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Performs a linear local PCA based on the covariance matrices of given
 * objects.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LinearLocalPCA extends LocalPCA {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  @SuppressWarnings({"UNUSED_SYMBOL", "FieldCanBeLocal"})
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * @see LocalPCA#sortedEigenPairs(de.lmu.ifi.dbs.database.Database, java.util.List)
   */
  protected SortedEigenPairs sortedEigenPairs(Database<RealVector> database, List<Integer> ids) {

    // covariance matrix
    Matrix covariance = Util.covarianceMatrix(database, ids);
    // eigen value decomposition
    EigenvalueDecomposition evd = covariance.eig();
    SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, false);

    if (DEBUG) {
      StringBuffer msg = new StringBuffer();
      msg.append("\ncov ").append(covariance);
      msg.append("\neigenpairs: ").append(Arrays.asList(eigenPairs));
      logger.fine(msg.toString());
    }

    return eigenPairs;
  }
}
