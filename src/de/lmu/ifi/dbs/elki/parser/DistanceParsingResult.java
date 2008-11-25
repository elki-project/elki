package de.lmu.ifi.dbs.elki.parser;

import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.utilities.pairs.SimplePair;

/**
 * Provides a list of database objects and labels associated with these objects
 * and a cache of precomputed distances between the database objects.
 *
 * @author Elke Achtert
 */
public class DistanceParsingResult<O extends DatabaseObject, D extends Distance<D>> extends ParsingResult<O> {
  /**
   * The cache of precomputed distances between the database objects.
   */
  private final Map<Integer, Map<Integer, D>> distanceCache;

  /**
   * Provides a list of database objects, a list of label obejcts associated
   * with these objects and cached distances between these objects.
   *
   * @param objectAndLabelList the list of database objects and labels associated with these
   *                           objects
   * @param distanceCache      the cache of precomputed distances between the database
   *                           objects
   */
  public DistanceParsingResult(List<SimplePair<O,List<String>>> objectAndLabelList,
                               Map<Integer, Map<Integer, D>> distanceCache) {
    super(objectAndLabelList);
    this.distanceCache = distanceCache;
  }

  /**
   * Returns the cache of precomputed distances between the database objects.
   *
   * @return the cache of precomputed distances between the database objects
   */
  public Map<Integer, Map<Integer, D>> getDistanceCache() {
    return distanceCache;
  }
}
