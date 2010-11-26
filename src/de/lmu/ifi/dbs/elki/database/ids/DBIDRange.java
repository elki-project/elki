package de.lmu.ifi.dbs.elki.database.ids;

/**
 * Static DBID range.
 * 
 * @author Erich Schubert
 */
public interface DBIDRange extends ArrayStaticDBIDs {
  /**
   * Get offset in the array for a particular DBID.
   * 
   * Should satisfy {@code range.get(getOffset(id)) == id} and
   * {@code range.getOffset(range.get(idx)) == idx}. 
   * 
   * @param dbid ID to compute index for
   * @return index
   */
  public int getOffset(DBID dbid);
}
