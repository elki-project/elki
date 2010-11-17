package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Abstract kNN Query interface. Usually you will want either a
 * {@link DBIDKNNQuery} or {@link ObjectKNNQuery} depending on the type of
 * queries you do.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public interface KNNQuery<O extends DatabaseObject, D extends Distance<D>> extends DatabaseQuery {
  /**
   * OptionID for the 'k' parameter
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("materialize.k", "The number of nearest neighbors of an object to be materialized.");

  /**
   * OptionID for the distance function
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("materialize.distance", "the distance function to materialize the nearest neighbors");

  /**
   * Get an instance for a particular database
   * 
   * @param database Database
   */
  public <T extends O> Instance<T, D> instantiate(Database<T> database);

  /**
   * Get the underlying distance function
   * 
   * @return get the distance function used.
   */
  @Deprecated
  public DistanceFunction<? super O, D> getDistanceFunction();

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
}