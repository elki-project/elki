package de.lmu.ifi.dbs.elki.index.tree.spatial;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.SpatialIndexKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.SpatialIndexRangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndex;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;

/**
 * Abstract super class for all spatial index classes.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has SpatialNode oneway - - contains
 * 
 * @param <O> Vector type
 * @param <N> Node type
 * @param <E> Entry type
 */
public abstract class SpatialIndex<O extends NumberVector<?, ?>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends TreeIndex<O, N, E> implements RangeIndex<O>, KNNIndex<O> {
  /**
   * If true, a bulk load will be performed.
   */
  protected boolean bulk;

  /**
   * The strategy for bulk load.
   */
  protected BulkSplit.Strategy bulkLoadStrategy;

  /**
   * Constructor. 
   * 
   * @param database Database
   * @param fileName file name
   * @param pageSize page size
   * @param cacheSize cache size
   * @param bulk bulk flag
   * @param bulkLoadStrategy bulk load strategy
   */
  public SpatialIndex(Database<O> database, String fileName, int pageSize, long cacheSize, boolean bulk, Strategy bulkLoadStrategy) {
    super(fileName, pageSize, cacheSize);
    // TODO: do we need the database?
    this.bulk = bulk;
    this.bulkLoadStrategy = bulkLoadStrategy;
  }

  @Override
  public <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(Database<O> database, DistanceFunction<? super O, D> distanceFunction, @SuppressWarnings("unused") Object... hints) {
    if(!(distanceFunction instanceof SpatialPrimitiveDistanceFunction)) {
      if(getLogger().isDebugging()) {
        getLogger().debug("Requested distance " + distanceFunction.toString() + " not supported by index.");
      }
      return null;
    }
    SpatialPrimitiveDistanceFunction<? super O, D> df = (SpatialPrimitiveDistanceFunction<? super O, D>) distanceFunction;
    DistanceQuery<O, D> dq = database.getDistanceQuery(distanceFunction);
    return new SpatialIndexKNNQuery<O, D>(database, this, dq, df);
  }

  @Override
  public <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(Database<O> database, DistanceQuery<O, D> distanceQuery, @SuppressWarnings("unused") Object... hints) {
    DistanceFunction<? super O, D> distanceFunction = distanceQuery.getDistanceFunction();
    if(!(distanceFunction instanceof SpatialPrimitiveDistanceFunction)) {
      if(getLogger().isDebugging()) {
        getLogger().debug("Requested distance " + distanceFunction.toString() + " not supported by index.");
      }
      return null;
    }
    SpatialPrimitiveDistanceFunction<? super O, D> df = (SpatialPrimitiveDistanceFunction<? super O, D>) distanceFunction;
    DistanceQuery<O, D> dq = database.getDistanceQuery(distanceFunction);
    return new SpatialIndexKNNQuery<O, D>(database, this, dq, df);
  }
  
  @Override
  public <D extends Distance<D>> RangeQuery<O, D> getRangeQuery(Database<O> database, DistanceFunction<? super O, D> distanceFunction, @SuppressWarnings("unused") Object... hints) {
    if(!(distanceFunction instanceof SpatialPrimitiveDistanceFunction)) {
      if(getLogger().isDebugging()) {
        getLogger().debug("Requested distance " + distanceFunction.toString() + " not supported by index.");
      }
      return null;
    }
    SpatialPrimitiveDistanceFunction<? super O, D> df = (SpatialPrimitiveDistanceFunction<? super O, D>) distanceFunction;
    DistanceQuery<O, D> dq = database.getDistanceQuery(distanceFunction);
    return new SpatialIndexRangeQuery<O, D>(database, this, dq, df);
  }

  @Override
  public <D extends Distance<D>> RangeQuery<O, D> getRangeQuery(Database<O> database, DistanceQuery<O, D> distanceQuery, @SuppressWarnings("unused") Object... hints) {
    DistanceFunction<? super O, D> distanceFunction = distanceQuery.getDistanceFunction();
    if(!(distanceFunction instanceof SpatialPrimitiveDistanceFunction)) {
      if(getLogger().isDebugging()) {
        getLogger().debug("Requested distance " + distanceFunction.toString() + " not supported by index.");
      }
      return null;
    }
    SpatialPrimitiveDistanceFunction<? super O, D> df = (SpatialPrimitiveDistanceFunction<? super O, D>) distanceFunction;
    DistanceQuery<O, D> dq = database.getDistanceQuery(distanceFunction);
    return new SpatialIndexRangeQuery<O, D>(database, this, dq, df);
  }
  
  /**
   * Performs a range query for the given object with the given epsilon range
   * and the according distance function. The query result is in ascending order
   * to the distance to the query object.
   * 
   * @param <D> distance type
   * @param obj the query object
   * @param epsilon the string representation of the query range
   * @param distanceFunction the distance function that computes the distances
   *        between the objects
   * @return a List of the query results
   */
  public abstract <D extends Distance<D>> List<DistanceResultPair<D>> rangeQuery(final O obj, final D epsilon, final SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction);

  /**
   * Performs a k-nearest neighbor query for the given object with the given
   * parameter k and the according distance function. The query result is in
   * ascending order to the distance to the query object.
   * 
   * @param <D> distance type
   * @param obj the query object
   * @param k the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances
   *        between the objects
   * @return a List of the query results
   */
  public abstract <D extends Distance<D>> List<DistanceResultPair<D>> kNNQuery(final O obj, final int k, final SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction);

  /**
   * Performs a bulk k-nearest neighbor query for the given object IDs. Each
   * query result is in ascending order to the distance to the query objects.
   * 
   * @param <D> distance type
   * @param ids the query objects
   * @param k the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances
   *        between the objects
   * @return a List of List the query results
   */
  // FIXME: Not yet supported. :-(
  public abstract <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkKNNQueryForIDs(DBIDs ids, final int k, final SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction);

  /**
   * Returns a list of entries pointing to the leaf nodes of this spatial index.
   * 
   * @return a list of entries pointing to the leaf nodes of this spatial index
   */
  public abstract List<E> getLeaves();
}