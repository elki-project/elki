package experimentalcode.erich.newdblayer.storage;

import experimentalcode.erich.newdblayer.ids.DBID;
import experimentalcode.erich.newdblayer.ids.DBIDRangeAllocation;

/**
 * Mapping a static DBID range to storage IDs.
 * 
 * @author Erich Schubert
 */
public class RangeIDMap implements StorageIDMap {
  /**
   * Start offset
   */
  final int start;
  
  /**
   * Constructor from a static DBID range allocation.
   * 
   * @param range DBID range to use
   */
  public RangeIDMap(DBIDRangeAllocation range) {
    this.start = range.start;
  }

  @Override
  public int map(DBID dbid) {
    return dbid.getIntegerID() - start;
  }
}
