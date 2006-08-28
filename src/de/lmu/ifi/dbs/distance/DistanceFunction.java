package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * Interface DistanceFunction describes the requirements of any distance
 * function.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface DistanceFunction<O extends DatabaseObject, D extends Distance> extends Parameterizable {
  /**
   * The default package for distance functions.
   */
  public static final String DEFAULT_PACKAGE = DistanceFunction.class.getPackage().getName();

  /**
   * Provides a distance suitable to this DistanceFunction based on the given
   * pattern.
   *
   * @param pattern A pattern defining a distance suitable to this
   *                DistanceFunction
   * @return a distance suitable to this DistanceFunction based on the given
   *         pattern
   * @throws IllegalArgumentException if the given pattern is not compatible with the requirements
   *                                  of this DistanceFunction
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

  /**
   * Returns a String as description of the required input format.
   *
   * @return a String as description of the required input format
   */
  String requiredInputPattern();

  /**
   * Computes the distance between two given DatabaseObjects according to this
   * distance function.
   *
   * @param o1 first DatabaseObject
   * @param o2 second DatabaseObject
   * @return the distance between two given DatabaseObjects according to this
   *         distance function
   */
  D distance(O o1, O o2);

  /**
   * Returns the distance between the two objcts specified by their obejct ids.
   *
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objcts specified by their obejct ids
   */
  D distance(Integer id1, Integer id2);

  /**
   * Returns the distance between the two specified objects.
   *
   * @param id1 first object id
   * @param o2 second DatabaseObject
   * @return the distance between the two objcts specified by their obejct ids
   */
  D distance(Integer id1, O o2);

  /**
   * Set the database that holds the associations for the DatabaseObject for
   * which the distances should be computed.
   *
   * @param database the database to be set
   * @param verbose  flag to allow verbose messages while performing the method
   * @param time     flag to request output of performance time
   */
  void setDatabase(Database<O> database, boolean verbose, boolean time);
}