package de.lmu.ifi.dbs.index.metrical;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.Index;
import de.lmu.ifi.dbs.utilities.QueryResult;

import java.util.List;

/**
 * Defines the requirements for a metrical index that can be used to efficiently store data.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public interface MetricalIndex<O extends MetricalObject, D extends Distance> extends Index<O> {
  /**
   * Performs a range query for the given object with the given
   * epsilon range and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param object  the query object
   * @param epsilon the string representation of the query range
   * @return a List of the query results
   */
  List<QueryResult<D>> rangeQuery(final O object, final String epsilon);

  /**
   * Performs a k-nearest neighbor query for the given object with the given
   * parameter k and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param object the query object
   * @param k      the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  List<QueryResult<D>> kNNQuery(final O object, final int k);

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   *
   * @param object the query object
   * @param k      the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  public List<QueryResult<D>> reverseKNNQuery(final O object, int k);

}
