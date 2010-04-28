package experimentalcode.erich.newdblayer.ids;

/**
 * Interface for array based DBIDs.
 * 
 * @author Erich Schubert
 */
public interface ArrayDBIDs extends DBIDs {
  /**
   * Get the i'th entry (starting at 0)
   * 
   * @param i Index
   * @return DBID of i'th entry.
   */
  public DBID get(int i);
}
