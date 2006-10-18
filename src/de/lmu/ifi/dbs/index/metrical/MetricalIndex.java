package de.lmu.ifi.dbs.index.metrical;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.index.Index;
import de.lmu.ifi.dbs.utilities.QueryResult;

import java.util.List;

/**
 * Abstract super class for all metrical index classes.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public abstract class MetricalIndex<O extends DatabaseObject, D extends Distance<D>, N extends MetricalNode<N,E>, E extends MetricalEntry> extends Index<O, N, E> {
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
  public abstract List<QueryResult<D>> rangeQuery(final O object, final String epsilon);

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
  public abstract List<QueryResult<D>> kNNQuery(final O object, final int k);

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   *
   * @param object the query object
   * @param k      the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  public abstract List<QueryResult<D>> reverseKNNQuery(final O object, int k);

  /**
   * Returns the distance function of this metrical index.
   *
   * @return the distance function of this metrical index
   */
  public abstract DistanceFunction<O, D> getDistanceFunction();

}
