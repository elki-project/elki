package de.lmu.ifi.dbs.elki.varianceanalysis;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

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
    return Util.covarianceMatrix(database, ids);
  }

  protected Matrix pcaMatrixResults(Database<V> database, Collection<QueryResult<DoubleDistance>> results) {
    Collection<Integer> ids = new ArrayList<Integer>(results.size());
    for (Iterator<QueryResult<DoubleDistance>> it = results.iterator(); it.hasNext(); ) {
      ids.add(it.next().getID());
    }
    return Util.covarianceMatrix(database, ids);
  }
}
