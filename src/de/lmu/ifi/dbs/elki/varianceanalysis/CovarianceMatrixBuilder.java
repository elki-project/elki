package de.lmu.ifi.dbs.elki.varianceanalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;

public abstract class CovarianceMatrixBuilder<V extends RealVector<V, ?>> extends AbstractParameterizable {
  /**
   * Compute Covariance Matrix for a complete database
   * 
   * @param ids a colleciton of ids
   * @param database the database used
   * @return Covariance Matrix
   * @throws UnableToComplyException
   */
  public Matrix processDatabase(Database<V> database) {
    return processIds(database.getIDs(), database);
  }

  /**
   * Compute Covariance Matrix for a collection of database IDs
   * 
   * @param ids a colleciton of ids
   * @param database the database used
   * @return Covariance Matrix
   */
  public abstract Matrix processIds(Collection<Integer> ids, Database<V> database);

  /**
   * Compute Covariance Matrix for a QueryResult Collection
   * 
   * By default it will just collect the ids and run processIds
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @param k the number of entries to process
   * @return Covariance Matrix
   */
  public Matrix processQueryResults(Collection<QueryResult<DoubleDistance>> results, Database<V> database, int k) {
    Collection<Integer> ids = new ArrayList<Integer>(k);
    int have = 0;
    for(Iterator<QueryResult<DoubleDistance>> it = results.iterator(); it.hasNext() && have < k; have++) {
      ids.add(it.next().getID());
    }
    return processIds(ids, database);
  }

  /**
   * Compute Covariance Matrix for a QueryResult Collection
   * 
   * By default it will just collect the ids and run processIds
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @return Covariance Matrix
   */
  final public Matrix processQueryResults(Collection<QueryResult<DoubleDistance>> results, Database<V> database) {
    return processQueryResults(results, database, results.size());
  }
}