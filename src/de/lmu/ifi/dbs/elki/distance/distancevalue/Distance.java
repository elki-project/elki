package de.lmu.ifi.dbs.elki.distance.distancevalue;

import java.io.Externalizable;

/**
 * The interface Distance defines the requirements of any instance class.
 * 
 * See {@link de.lmu.ifi.dbs.elki.distance.DistanceUtil} for related utility
 * functions such as <code>min</code>, <code>max</code>.
 * 
 * @author Arthur Zimek
 * @see de.lmu.ifi.dbs.elki.distance.DistanceUtil
 * @param <D> the type of Distance used
 */
public interface Distance<D extends Distance<D>> extends Comparable<D>, Externalizable {
  /**
   * Returns a new distance as sum of this distance and the given distance.
   * 
   * @param distance the distance to be added to this distance
   * @return a new distance as sum of this distance and the given distance
   */
  D plus(D distance);

  /**
   * Returns a new Distance by subtracting the given distance from this
   * distance.
   * 
   * @param distance the distance to be subtracted from this distance
   * @return a new Distance by subtracting the given distance from this distance
   */
  D minus(D distance);

  /**
   * Any implementing class should implement a proper toString-method for
   * printing the result-values.
   * 
   * @return String a human-readable representation of the Distance
   */
  String toString();

  /**
   * Provides a measurement suitable to this measurement function based on the
   * given pattern.
   * 
   * @param pattern a pattern defining a similarity suitable to this measurement
   *        function
   * @return a measurement suitable to this measurement function based on the
   *         given pattern
   * @throws IllegalArgumentException if the given pattern is not compatible
   *         with the requirements of this measurement function
   */
  D parseString(String pattern) throws IllegalArgumentException;

  /**
   * Returns a String as description of the required input format.
   * 
   * @return a String as description of the required input format
   */
  String requiredInputPattern();

  /**
   * Returns the number of Bytes this distance uses if it is written to an
   * external file.
   * 
   * @return the number of Bytes this distance uses if it is written to an
   *         external file
   */
  int externalizableSize();

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
   * Returns true, if the distance is an infinite distance, false otherwise.
   * 
   * @return true, if the distance is an infinite distance, false otherwise
   */
  boolean isInfiniteDistance();

  /**
   * Returns true, if the distance is a null distance, false otherwise.
   * 
   * @return true, if the distance is a null distance, false otherwise
   */
  boolean isNullDistance();

  /**
   * Returns true, if the distance is an undefined distance, false otherwise.
   * 
   * @return true, if the distance is an undefined distance, false otherwise
   */
  boolean isUndefinedDistance();
}