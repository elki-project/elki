package de.lmu.ifi.dbs.elki.datasource.parser;

import java.util.Map;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Provides a list of database objects and labels associated with these objects
 * and a cache of precomputed distances between the database objects.
 * 
 * @author Elke Achtert
 * @param <O> object type
 * @param <D> distance type
 */
public class DistanceParsingResult<D extends Distance<D>> {
  /**
   * The cache of precomputed distances between the database objects.
   */
  private final Map<DBIDPair, D> distanceCache;
  
  /**
   * Objects representation (DBIDs and/or external IDs)
   */
  private MultipleObjectsBundle objects;

  /**
   * Provides a list of database objects, a list of label objects associated
   * with these objects and cached distances between these objects.
   * 
   * @param objectAndLabelList the list of database objects and labels
   *        associated with these objects
   * @param distanceCache the cache of precomputed distances between the
   *        database objects
   */
  public DistanceParsingResult(MultipleObjectsBundle objectAndLabelList, Map<DBIDPair, D> distanceCache) {
    this.objects = objectAndLabelList;
    this.distanceCache = distanceCache;
  }

  /**
   * Returns the cache of precomputed distances between the database objects.
   * 
   * @return the cache of precomputed distances between the database objects
   */
  public Map<DBIDPair, D> getDistanceCache() {
    return distanceCache;
  }

  /**
   * Get the objects
   * 
   * @return
   */
  public MultipleObjectsBundle getObjects() {
    return objects;
  }
}