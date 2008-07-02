package de.lmu.ifi.dbs.elki.varianceanalysis;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.Util;

import java.util.Collection;

/**
 * Performs a linear local PCA based on the covariance matrices of given
 * objects.
 *
 * @author Elke Achtert 
 */
public class LinearLocalPCA<V extends RealVector<V,?>> extends LocalPCA<V> {

  /**
   * Returns the covariance matrix of the specified ids.
   *
   * @see LocalPCA#pcaMatrix(de.lmu.ifi.dbs.elki.database.Database, java.util.Collection)
   */
  protected Matrix pcaMatrix(Database<V> database, Collection<Integer> ids) {
    // covariance matrix
    return Util.covarianceMatrix(database, ids);
  }
}
