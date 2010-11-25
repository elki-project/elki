package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.MkTreeRKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * Abstract class for all M-Tree variants supporting processing of reverse
 * k-nearest neighbor queries by using the k-nn distances of the entries, where
 * k is less than or equal to the given parameter.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 * @param <D> the type of Distance used in the metrical index
 * @param <N> the type of MetricalNode used in the metrical index
 * @param <E> the type of MetricalEntry used in the metrical index
 */
public abstract class AbstractMkTree<O extends DatabaseObject, D extends Distance<D>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry<D>> extends AbstractMTree<O, D, N, E> implements RKNNIndex<O> {
  /**
   * Constructor.
   * 
   * @param fileName file name
   * @param pageSize page size
   * @param cacheSize cache size
   * @param distanceQuery Distance query
   * @param distanceFunction Distance function
   */
  public AbstractMkTree(String fileName, int pageSize, long cacheSize, DistanceQuery<O, D> distanceQuery, DistanceFunction<O, D> distanceFunction) {
    super(fileName, pageSize, cacheSize, distanceQuery, distanceFunction);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> RKNNQuery<O, S> getRKNNQuery(Database<O> database, DistanceFunction<? super O, S> distanceFunction, Object... hints) {
    if(!this.getDistanceFunction().equals(distanceFunction)) {
      if(getLogger().isDebugging()) {
        getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    // Bulk is not yet supported
    for (Object hint : hints) {
      if (hint == DatabaseQuery.HINT_BULK) {
        return null;
      }
    }
    AbstractMkTreeUnified<O, S, ?, ?> idx = (AbstractMkTreeUnified<O, S, ?, ?>) this;
    DistanceQuery<O, S> dq = database.getDistanceQuery(distanceFunction);
    return new MkTreeRKNNQuery<O, S>(database, idx, dq);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> RKNNQuery<O, S> getRKNNQuery(Database<O> database, DistanceQuery<O, S> distanceQuery, Object... hints) {
    DistanceFunction<? super O, S> distanceFunction = distanceQuery.getDistanceFunction();
    if(!this.getDistanceFunction().equals(distanceFunction)) {
      if(getLogger().isDebugging()) {
        getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    // Bulk is not yet supported
    for (Object hint : hints) {
      if (hint == DatabaseQuery.HINT_BULK) {
        return null;
      }
    }
    AbstractMkTreeUnified<O, S, ?, ?> idx = (AbstractMkTreeUnified<O, S, ?, ?>) this;
    DistanceQuery<O, S> dq = database.getDistanceQuery(distanceFunction);
    return new MkTreeRKNNQuery<O, S>(database, idx, dq);
  }
  
  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   * 
   * @param object the query object
   * @param k the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  public abstract List<DistanceResultPair<D>> reverseKNNQuery(final O object, int k);
}