package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * The interface of an actual instance.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has de.lmu.ifi.dbs.elki.database.query.DistanceResultPair oneway - - returns
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public interface KNNQuery<O extends DatabaseObject, D extends Distance<D>> extends DatabaseQuery {
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
   * Get the k nearest neighbors for a particular id.
   * 
   * @param obj Query object
   * @param k Number of neighbors requested
   * @return neighbors
   */
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
}