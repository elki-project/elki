package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
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
public interface DBIDKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends Parameterizable, KNNQuery<O, D> {
  /**
   * Get an instance for a particular database
   * 
   * @param database Database
   */
  @Override
  public <T extends O> Instance<T, D> instantiate(Database<T> database);

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
     * @param id query object ID
     * @return neighbors
     */
    public List<DistanceResultPair<D>> getForDBID(DBID id);
  }
}