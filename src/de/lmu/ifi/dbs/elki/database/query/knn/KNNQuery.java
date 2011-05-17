package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;

/**
 * The interface of an actual instance.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses DistanceResultPair oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public interface KNNQuery<O, D extends Distance<D>> extends DatabaseQuery {
  /**
   * Get the k nearest neighbors for a particular id.
   * 
   * @param id query object ID
   * @param k Number of neighbors requested
   * @return neighbors
   */
  public List<DistanceResultPair<D>> getKNNForDBID(DBID id, int k);

  /**
   * Bulk query method
   * 
   * @param ids query object IDs
   * @param k Number of neighbors requested
   * @return neighbors
   */
  public List<List<DistanceResultPair<D>>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k);

  /**
   * Bulk query method configured by a map.
   * 
   * Warning: this API is not optimal, and might be removed soon (in fact, it is
   * used in a single place)
   * 
   * @param heaps Map of heaps to fill.
   */
  public void getKNNForBulkHeaps(Map<DBID, KNNHeap<D>> heaps);

  /**
   * Get the k nearest neighbors for a particular id.
   * 
   * @param obj Query object
   * @param k Number of neighbors requested
   * @return neighbors
   */
  // TODO: return KNNList<D> instead?
  public List<DistanceResultPair<D>> getKNNForObject(O obj, int k);

  /**
   * Get the distance query for this function.
   */
  // TODO: remove?
  public DistanceQuery<O, D> getDistanceQuery();

  /**
   * Get the distance data type of the function.
   */
  public D getDistanceFactory();

  /**
   * Access the underlying data query.
   * 
   * @return data query in use
   */
  public abstract Relation<? extends O> getRelation();
}