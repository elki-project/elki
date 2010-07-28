package de.lmu.ifi.dbs.elki.database.query;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface to abstract a kNN query (with fixed k!). This allows replacing the
 * kNN query with approximations or the use of preprocessors.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance
 */
public interface KNNQuery<O extends DatabaseObject, D extends Distance<D>> extends Parameterizable {
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
   * Get an instance for a particular database
   * 
   * @param database Database
   * @param distanceQuery Distance Query
   */
  public <T extends O> Instance<T, D> instantiate(Database<T> database, DistanceQuery<T, D> distanceQuery);

  /**
   * Get the underlying distance function
   * 
   * @return get the distance function used.
   */
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
     * @return neighbors
     */
    public List<DistanceResultPair<D>> get(DBID id);

    /**
     * Get the distance data type of the function.
     */
    public DistanceQuery<O, D> getDistanceQuery();
  }
}