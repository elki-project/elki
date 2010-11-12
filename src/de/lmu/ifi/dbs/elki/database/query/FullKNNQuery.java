package de.lmu.ifi.dbs.elki.database.query;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * KNNQuerys that can process both object IDs and full objects
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public interface FullKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends DBIDKNNQuery<O, D>, ObjectKNNQuery<O, D> {
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
  public static interface Instance<O extends DatabaseObject, D extends Distance<D>> extends DBIDKNNQuery.Instance<O, D>, ObjectKNNQuery.Instance<O, D> {
    // Empty - no additional methods, but must implement both interfaces!
  }
}