package de.lmu.ifi.dbs.varianceanalysis;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.Collection;

/**
 * Performs a linear local PCA based on the covariance matrices of given
 * objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LinearLocalPCA extends LocalPCA {

  /**
   * Returns the covariance matrix of the specified ids.
   *
   * @see LocalPCA#pcaMatrix(de.lmu.ifi.dbs.database.Database, java.util.Collection)
   */
  protected Matrix pcaMatrix(Database<RealVector> database, Collection<Integer> ids) {
    // covariance matrix
    return Util.covarianceMatrix(database, ids);
  }
}
