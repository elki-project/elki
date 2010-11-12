package de.lmu.ifi.dbs.elki.database.query;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Interface to abstract a kNN query (with fixed k!). This allows replacing the
 * kNN query with approximations or the use of preprocessors.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance
 */
public interface ObjectKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends KNNQuery<O,D> {
  /**
   * Get an instance for a particular database
   * 
   * @param database Database
   */
  @Override
  public <T extends O> Instance<T, D> instantiate(Database<T> database);

  /**
   * Get the underlying distance function
   * 
   * @return get the distance function used.
   */
  public DistanceFunction<? super O, D> getDistanceFunction();

  /**
   * The interface of an actual instance.
   * 
   * @author Erich Schubert
   * 
   * @param <O> Object type
   * @param <D> Distance type
   */
  public static interface Instance<O extends DatabaseObject, D extends Distance<D>> extends KNNQuery.Instance<O, D> {
    /**
     * Get the k nearest neighbors for a particular id.
     * 
     * @param obj Query object
     * @return neighbors
     */
    public List<DistanceResultPair<D>> getForObject(O obj);

    /**
     * Get the distance data type of the function.
     */
    public DistanceQuery<O, D> getDistanceQuery();
  }
}