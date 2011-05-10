package experimentalcode.frankenb.algorithms.partitioning;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Simple partition class
 * 
 * In the simplest case, a partitions just contains some object IDs.
 * 
 * @author Erich Schubert
 */
public class DBIDPartition {
  /**
   * Contained object ids
   */
  protected DBIDs ids;
  
  /**
   * Constructor.
   *
   * @param ids
   */
  public DBIDPartition(DBIDs ids) {
    super();
    this.ids = ids;
  }

  /**
   * @return the ids
   */
  public DBIDs getDBIDs() {
    return ids;
  }
  
  /**
   * Get the partition size.
   * 
   * @return size
   */
  public int getSize() {
    return ids.size();
  }
}
