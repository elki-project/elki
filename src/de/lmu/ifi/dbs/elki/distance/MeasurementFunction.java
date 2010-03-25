package de.lmu.ifi.dbs.elki.distance;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;

/**
 * Interface Measurement describes the requirements of any measurement
 * function (e.g. distance function or similarity function), that provides a measurement
 * for comparing database objects.
 *
 * @author Elke Achtert
 * @param <D> the type of Distance used as measurement for comparing database objects
 * @param <O> the type of DatabaseObject for which a measurement is provided for comparison
 */
public interface MeasurementFunction<O extends DatabaseObject, D extends Distance<D>> {
  /**
   * Set the database that holds the associations for the DatabaseObject for
   * which the measurements should be computed.
   *
   * @param database the database to be set
   */
  void setDatabase(Database<O> database);

  /**
   * Returns a String as description of the required input format.
   *
   * @return a String as description of the required input format
   */
  String requiredInputPattern();

  /**
   * Provides a measurement suitable to this measurement function based on the given
   * pattern.
   *
   * @param pattern a pattern defining a similarity suitable to this
   *                measurement function
   * @return a measurement suitable to this measurement function based on the given
   *         pattern
   * @throws IllegalArgumentException if the given pattern is not compatible with the requirements
   *                                  of this measurement function
   */
  D valueOf(String pattern) throws IllegalArgumentException;

  /**
   * Provides an infinite distance.
   *
   * @return an infinite distance
   */
  D infiniteDistance();

  /**
   * Provides a null distance.
   *
   * @return a null distance
   */
  D nullDistance();

  /**
   * Provides an undefined distance.
   *
   * @return an undefined distance
   */
  D undefinedDistance();

  /**
   * Returns true, if the given distance is an infinite distance, false
   * otherwise.
   *
   * @param distance the distance to be tested on infinity
   * @return true, if the given distance is an infinite distance, false
   *         otherwise
   */
  boolean isInfiniteDistance(D distance);

  /**
   * Returns true, if the given distance is a null distance, false otherwise.
   *
   * @param distance the distance to be tested whether it is a null distance
   * @return true, if the given distance is a null distance, false otherwise
   */
  boolean isNullDistance(D distance);

  /**
   * Returns true, if the given distance is an undefined distance, false
   * otherwise.
   *
   * @param distance the distance to be tested whether it is undefined
   * @return true, if the given distance is an undefined distance, false
   *         otherwise
   */
  boolean isUndefinedDistance(D distance);
}
