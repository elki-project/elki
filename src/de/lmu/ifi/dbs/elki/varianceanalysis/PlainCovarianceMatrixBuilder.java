package de.lmu.ifi.dbs.elki.varianceanalysis;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.Util;

public class PlainCovarianceMatrixBuilder<V extends RealVector<V, ?>> extends CovarianceMatrixBuilder<V> {
  /**
   * Compute Covariance Matrix for a complete database
   * 
   * @param ids a colleciton of ids
   * @param database the database used
   * @return Covariance Matrix
   */
  public Matrix processDatabase(Database<V> database) {
    return Util.covarianceMatrix(database);
  }

  /**
   * Compute Covariance Matrix for a collection of database IDs
   * 
   * @param ids a colleciton of ids
   * @param database the database used
   * @return Covariance Matrix
   */
  public Matrix processIds(Collection<Integer> ids, Database<V> database) {
    return Util.covarianceMatrix(database, ids);
  }
}
