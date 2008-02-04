package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.distance.Distance;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a cache for distances between database objects.
 *
 * @author Elke Achtert 
 */
public class DistanceCache<D extends Distance<D>> {
  /**
   * The map holding the pairwise distances.
   */
  private Map<Integer, Map<Integer, D>> cache = new HashMap<Integer, Map<Integer, D>>();

  /**
   * Puts the specified distance value for the given ids to cache.
   *
   * @param id1      the first id
   * @param id2      the second id
   * @param distance the distance value
   */
  public void put(Integer id1, Integer id2, D distance) {
    // the smaller id is the first key
    if (id1 < id2) {
      put(id2, id1, distance);
    }

    Map<Integer, D> distances = cache.get(id1);
    if (distances == null) {
      distances = new HashMap<Integer, D>();
      cache.put(id1, distances);
    }

    D oldDistance = distances.put(id2, distance);

    if (oldDistance != null) {
      throw new IllegalArgumentException("Distance value for specified ids is already assigned!");
    }
  }

  /**
   * Returns the distance value of the specified ids if it has been cached, null otherwise.
   *
   * @param id1 the first id
   * @param id2 the second id
   * @return the distance value of the specified ids if it has been cached, null otherwise
   */
  public D get(Integer id1, Integer id2) {
    if (id1 < id2) {
      return get(id2, id1);
    }

    Map<Integer, D> distances = cache.get(id1);
    if (distances == null) {
      return null;
    }
    return distances.get(id2);
  }

  /**
   * Returns <tt>true</tt> if this cache contains a distance value for the specified
   * ids.
   *
   * @param id1 the first id
   * @param id2 the second id
   * @return <tt>true</tt> if this cache contains a distance value for the specified
   *         ids, false otherwise
   */
  public boolean containsKey(Integer id1, Integer id2) {
    if (id1 < id2) {
      return containsKey(id2, id1);
    }

    Map<Integer, D> distances = cache.get(id1);
    if (distances == null) {
      return false;
    }
    return distances.containsKey(id2);
  }

}
