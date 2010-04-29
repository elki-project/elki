package experimentalcode.erich.newdblayer.ids;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;

/**
 * Database ID object.
 * 
 * While this currently is just an Integer, it should be avoided to store the
 * object IDs in regular integers to reduce problems if this API ever changes
 * (for example if someone needs to do context tracking for debug purposes!)
 * 
 * In particular, a developer should not make any assumption of these IDs being
 * consistent across multiple results/databases.
 * 
 * @author Erich Schubert
 */
// TODO: remove "implements DatabaseObject", getID and setID.
public interface DBID extends DatabaseObject, Comparable<DBID>, DBIDs {
  /**
   * Return the integer value of the object ID, if possible.
   * 
   * @return integer id
   */
  public int getIntegerID();  
}