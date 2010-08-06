package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Base interface for any kind of distances.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 * @param <D> Distance result type
 */
public interface DistanceFunction<O extends DatabaseObject, D extends Distance<D>> extends Parameterizable {
  /**
   * Method to get the distance functions factory.
   * 
   * @return Factory for distance objects
   */
  D getDistanceFactory();

  /**
   * Is this function symmetric?
   * 
   * @return {@code true} when symmetric
   */
  boolean isSymmetric();

  /**
   * Is this distance function metric (in particular, does it satisfy the
   * triangle equation?)
   * 
   * @return {@code true} when metric.
   */
  boolean isMetric();

  /**
   * Get the input data type of the function.
   */
  Class<? super O> getInputDatatype();

  /**
   * Instantiate with a database to get the actual distance query.
   * 
   * @param database
   * @return Actual distance query.
   */
  public <T extends O> DistanceQuery<T, D> instantiate(Database<T> database);
}