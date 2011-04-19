package de.lmu.ifi.dbs.elki.database.ids;


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
 * 
 * @apiviz.landmark
 */
public interface DBID extends Comparable<DBID>, ArrayStaticDBIDs {
  /**
   * Return the integer value of the object ID, if possible.
   * 
   * @return integer id
   */
  public int getIntegerID();  
}