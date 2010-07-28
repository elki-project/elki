package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Distance functions valid in a database context only (i.e. for DBIDs)
 * 
 * For any "distance" that cannot be computed for arbitrary objects, only those
 * that exist in the database and referenced by their ID.
 * 
 * Example: external precomputed distances
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
public interface DBIDDistanceFunction<D extends Distance<D>> extends DistanceFunction<DatabaseObject,D> {
  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objects specified by their object ids
   */
  D distance(DBID id1, DBID id2);
}