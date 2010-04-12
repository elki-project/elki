package experimentalcode.erich.newdblayer.storage;

import experimentalcode.erich.newdblayer.ids.DBID;

/**
 * Interface to map DBIDs to integer record ids for use in storage.
 * 
 * @author Erich Schubert
 */
public interface StorageIDMap {
  /**
   * Map a DBID to a database id.
   * 
   * @param dbid DBID
   * @return record id {@code id >= 0}
   */
  public int map(DBID dbid);
}
