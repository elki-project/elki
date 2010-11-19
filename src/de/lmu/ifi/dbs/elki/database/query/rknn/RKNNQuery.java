package de.lmu.ifi.dbs.elki.database.query.rknn;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Abstract reverse kNN Query interface.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public interface RKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends DatabaseQuery {
  /**
   * Get the reverse k nearest neighbors for a particular id.
   * 
   * @param id query object ID
   * @param k number of neighbors requested
   * @return reverse k nearest neighbors
   */
  public List<DistanceResultPair<D>> getRKNNForDBID(DBID id, int k);

  /**
   * Get the reverse k nearest neighbors for a particular object.
   * 
   * @param obj query object instance
   * @param k number of neighbors requested
   * @return reverse k nearest neighbors
   */
  public List<DistanceResultPair<D>> getRKNNForObject(O obj, int k);

  /**
   * Bulk query method for reverse k nearest neighbors for ids.
   * 
   * @param ids query object IDs
   * @param k number of neighbors requested
   * @return reverse k nearest neighbors
   */
  public List<List<DistanceResultPair<D>>> getRKNNForBulkDBIDs(ArrayDBIDs ids, int k);

  /**
   * Get the distance data type of the function.
   */
  public D getDistanceFactory();
}