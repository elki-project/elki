package experimentalcode.erich.newdblayer.ids;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Simple DBID management, that never reuses IDs.
 * Statically allocated DBID ranges are given positive values,
 * Dynamically allocated DBIDs are given negative values.
 * 
 * @author Erich Schubert
 */
public class SimpleDBIDFactory implements DBIDFactory {
  /**
   * Logging for error messages.
   */
  Logging logger = Logging.getLogger(SimpleDBIDFactory.class);
  
  /**
   * Keep track of the smallest dynamic DBID offset not used
   */
  int dynamicids = 0;
  
  /**
   * The starting point for static DBID range allocations.
   */
  int rangestart = 0;
  
  /**
   * Constructor
   */
  public SimpleDBIDFactory() {
    super();
  }

  @Override
  public synchronized DBID generateSingleDBID() {
    if (dynamicids == Integer.MIN_VALUE) {
      throw new AbortException("DBID range allocation error - too many objects allocated!");
    }
    dynamicids--;
    return new DBID(dynamicids);
  }

  @Override
  public void deallocateSingleDBID(@SuppressWarnings("unused") DBID id) {
    // ignore.
  }

  @Override
  public synchronized DBIDRangeAllocation generateStaticDBIDRange(int size) {
    if (rangestart >= Integer.MAX_VALUE - size) {
      throw new AbortException("DBID range allocation error - too many objects allocated!");
    }
    DBIDRangeAllocation alloc = new DBIDRangeAllocation(rangestart, size);
    rangestart += size;
    return alloc;
  }

  @Override
  public void deallocateDBIDRange(@SuppressWarnings("unused") DBIDRangeAllocation range) {
    // ignore.
  }
}