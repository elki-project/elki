package de.lmu.ifi.dbs.elki.database.datastore;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.RangeDBIDs;

/**
 * Mapping a static DBID range to storage IDs.
 * 
 * @author Erich Schubert
 */
public class RangeIDMap implements DataStoreIDMap {
  /**
   * Start offset
   */
  final RangeDBIDs range;
  
  /**
   * Constructor from a static DBID range allocation.
   * 
   * @param range DBID range to use
   */
  public RangeIDMap(RangeDBIDs range) {
    this.range = range;
  }

  @Override
  public int map(DBID dbid) {
    return range.getOffset(dbid);
  }
}
