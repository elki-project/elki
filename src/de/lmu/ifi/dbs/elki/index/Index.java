package de.lmu.ifi.dbs.elki.index;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface defining the minimum requirements for all index classes.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to be stored in the index
 */
public interface Index<O extends DatabaseObject> extends Parameterizable, AnyResult {
  /**
   * Returns the physical read access of this index.
   * 
   * @return the number of pages read from hard disk since the last call of
   *         <code>resetPageAccess</code>.
   */
  public long getPhysicalReadAccess();

  /**
   * Returns the physical write access of this index.
   * 
   * @return the number of pages written to hard disk since the last call of
   *         <code>resetPageAccess</code>.
   */
  public long getPhysicalWriteAccess();

  /**
   * Returns the logical page access of this index.
   * 
   * @return the overall number of pages accesses (including e.g. cache
   *         operations like put or remove) since the last call of
   *         <code>resetPageAccess</code>.
   */
  public long getLogicalPageAccess();

  /**
   * Resets the three counters for page access, i.e., the counters for physical
   * read and write access, and the counter for logical page access.
   */
  public void resetPageAccess();

  /**
   * Closes this index.
   */
  public void close();

  /**
   * Inserts the specified object into this index.
   * 
   * @param object the vector to be inserted
   */
  public void insert(O object);

  /**
   * Inserts the specified objects into this index. If a bulk load mode is
   * implemented, the objects are inserted in one bulk.
   * 
   * @param objects the objects to be inserted
   */
  public void insert(List<O> objects);

  /**
   * Deletes the specified object from this index.
   * 
   * @param object the object to be deleted
   * @return true if this index did contain the object, false otherwise
   */
  public boolean delete(O object);

  /**
   * Sets the database in the distance function of this index (if existing).
   * 
   * @param database the database
   */
  public void setDatabase(Database<O> database);

  /**
   * Get a KNN query object for the given distance function and k.
   * 
   * This function MAY return null, when the given distance is not supported!
   * 
   * @param <D> Distance type
   * @param database Database
   * @param distanceFunction Distance function
   * @param hints Hints for the optimizer
   * @return KNN Query object or {@code null}
   */
  // TODO: Do we really need the database?
  <D extends Distance<D>> KNNQuery.Instance<O, D> getKNNQuery(Database<O> database, DistanceFunction<? super O, D> distanceFunction, Object... hints);

  /**
   * Get a KNN query object for the given distance query and k.
   * 
   * This function MAY return null, when the given distance is not supported!
   * 
   * @param <D> Distance type
   * @param database Database
   * @param distanceQuery Distance query
   * @param hints Hints for the optimizer
   * @return KNN Query object or {@code null}
   */
  // TODO: Do we really need the database?
  <D extends Distance<D>> KNNQuery.Instance<O, D> getKNNQuery(Database<O> database, DistanceQuery<O, D> distanceQuery, Object... hints);

  /**
   * Get a range query object for the given distance function and k.
   * 
   * This function MAY return null, when the given distance is not supported!
   * 
   * @param <D> Distance type
   * @param database Database
   * @param distanceFunction Distance function
   * @param hints Hints for the optimizer
   * @return KNN Query object or {@code null}
   */
  // TODO: Do we really need the database?
  <D extends Distance<D>> RangeQuery.Instance<O, D> getRangeQuery(Database<O> database, DistanceFunction<? super O, D> distanceFunction, Object... hints);

  /**
   * Get a range query object for the given distance query and k.
   * 
   * This function MAY return null, when the given distance is not supported!
   * 
   * @param <D> Distance type
   * @param database Database
   * @param distanceQuery Distance query
   * @param hints Hints for the optimizer
   * @return KNN Query object or {@code null}
   */
  // TODO: Do we really need the database?
  <D extends Distance<D>> RangeQuery.Instance<O, D> getRangeQuery(Database<O> database, DistanceQuery<O, D> distanceQuery, Object... hints);
}