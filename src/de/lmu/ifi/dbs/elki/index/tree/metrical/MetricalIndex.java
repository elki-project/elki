package de.lmu.ifi.dbs.elki.index.tree.metrical;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndex;

/**
 * Abstract super class for all metrical index classes.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalNode oneway - - contains
 * 
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 * @param <D> the type of Distance used in the metrical index
 * @param <N> the type of MetricalNode used in the metrical index
 * @param <E> the type of MetricalEntry used in the metrical index
 */
public abstract class MetricalIndex<O, D extends Distance<D>, N extends MetricalNode<N, E>, E extends MetricalEntry> extends TreeIndex<O, N, E> implements KNNIndex<O>, RangeIndex<O> {
  /**
   * Constructor.
   * 
   * @param fileName file name
   * @param pageSize page size
   * @param cacheSize cache size
   */
  public MetricalIndex(Relation<O> representation, String fileName, int pageSize, long cacheSize) {
    super(representation, fileName, pageSize, cacheSize);
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
   * Returns the distance function of this metrical index.
   * 
   * @return the distance function of this metrical index
   */
  public abstract DistanceFunction<? super O, D> getDistanceFunction();
  
  /**
   * Returns the distance function of this metrical index.
   * 
   * @return the distance function of this metrical index
   */
  public abstract DistanceQuery<O, D> getDistanceQuery();
  
  /**
   * Returns a list of entries pointing to the leaf nodes of this spatial index.
   * 
   * @return a list of entries pointing to the leaf nodes of this spatial index
   */
  public abstract List<E> getLeaves();  
  
  
}