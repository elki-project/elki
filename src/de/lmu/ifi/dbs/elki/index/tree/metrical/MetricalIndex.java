package de.lmu.ifi.dbs.elki.index.tree.metrical;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndex;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Abstract super class for all metrical index classes.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 * @param <D> the type of Distance used in the metrical index
 * @param <N> the type of MetricalNode used in the metrical index
 * @param <E> the type of MetricalEntry used in the metrical index
 */
public abstract class MetricalIndex<O extends DatabaseObject, D extends Distance<D>, N extends MetricalNode<N, E>, E extends MetricalEntry> extends TreeIndex<O, N, E> {
  /**
   * Constructor
   * 
   * @param config Configuration
   */
  public MetricalIndex(Parameterization config) {
    super(config);
  }

  /**
   * Performs a range query for the given object with the given epsilon range
   * and the according distance function. The query result is in ascending order
   * to the distance to the query object.
   * 
   * @param object the query object
   * @param epsilon the string representation of the query range
   * @return a List of the query results
   */
  public abstract List<DistanceResultPair<D>> rangeQuery(final O object, final D epsilon);

  /**
   * Performs a k-nearest neighbor query for the given object with the given
   * parameter k and the according distance function. The query result is in
   * ascending order to the distance to the query object.
   * 
   * @param object the query object
   * @param k the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  public abstract List<DistanceResultPair<D>> kNNQuery(final O object, final int k);

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   * 
   * @param object the query object
   * @param k the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  public abstract List<DistanceResultPair<D>> reverseKNNQuery(final O object, int k);

  /**
   * Returns the distance function of this metrical index.
   * 
   * @return the distance function of this metrical index
   */
  public abstract DistanceFunction<O, D> getDistanceFunction();
}