package experimentalcode.erich.newdblayer.ids;

/**
 * Factory interface for generating DBIDs. See {@link #FACTORY} for the static
 * instance to use.
 * 
 * @author Erich Schubert
 */
public interface DBIDFactory {
  /**
   * Static DBID factory to use.
   */
  public static DBIDFactory FACTORY = new SimpleDBIDFactory();

  /**
   * Generate a single DBID
   * 
   * @return A single DBID
   */
  public DBID generateSingleDBID();

  /**
   * Return a single DBID for reuse.
   * 
   * @param id DBID to deallocate
   */
  public void deallocateSingleDBID(DBID id);

  /**
   * Generate a static DBID range.
   * 
   * @param size Requested size
   * @return DBID range
   */
  public DBIDRangeAllocation generateStaticDBIDRange(int size);

  /**
   * Deallocate a static DBID range.
   * 
   * @param range Range to deallocate
   */
  public void deallocateDBIDRange(DBIDRangeAllocation range);
}