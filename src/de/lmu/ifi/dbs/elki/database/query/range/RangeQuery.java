package de.lmu.ifi.dbs.elki.database.query.range;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Abstract range Query interface.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public interface RangeQuery<O extends DatabaseObject, D extends Distance<D>> {
  /**
   * Optimizer hint: request bulk support.
   */
  public static final String BULK_HINT = "need_bulk";
  
  /**
   * Optimizer hint: only a single request will be done - avoid expensive optimizations
   */
  public static final String SINGLE_QUERY = "single_query";
  
  /**
   * Optimizer hint: no linear scans allowed - return null then!
   */
  public static final String OPTIMIZED_ONLY = "optimized";
  
  /**
   * Optimizer hint: heavy use - caching/preprocessing/approximation recommended
   */
  public static final String HEAVY = "heavy";
  
  /**
   * Optimizer hint: exact - no approximations allowed!
   */
  public static final String EXACT = "exact";
  
  /**
   * Get an instance for a particular database
   * 
   * @param database Database
   */
  public <T extends O> Instance<T, D> instantiate(Database<T> database);

  /**
   * Get the distance data type of the function.
   */
  public D getDistanceFactory();

  /**
   * The interface of an actual instance.
   * 
   * @author Erich Schubert
   * 
   * @param <O> Object type
   * @param <D> Distance type
   */
  public static interface Instance<O extends DatabaseObject, D extends Distance<D>> {
    /**
     * Get the nearest neighbors for a particular id in a given query range
     * 
     * @param id query object ID
     * @param range Query range
     * @return neighbors
     */
    public List<DistanceResultPair<D>> getRangeForDBID(DBID id, D range);

    /**
     * Bulk query method
     * 
     * @param ids query object IDs
     * @param range Query range
     * @return neighbors
     */
    public List<List<DistanceResultPair<D>>> getRangeForBulkDBIDs(ArrayDBIDs ids, D range);

    /**
     * Get the nearest neighbors for a particular object in a given query range
     * 
     * @param obj Query object
     * @param range Query range
     * @return neighbors
     */
    public List<DistanceResultPair<D>> getRangeForObject(O obj, D range);

    /**
     * Get the distance data type of the function.
     */
    public D getDistanceFactory();
  }
}