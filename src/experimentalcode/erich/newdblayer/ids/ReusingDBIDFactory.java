package experimentalcode.erich.newdblayer.ids;

import java.util.ArrayList;
import java.util.BitSet;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Slightly more advanced DBID management, that allows reuse of DBIDs.
 * 
 * @author Erich Schubert
 */
public class ReusingDBIDFactory extends SimpleDBIDFactory {
  /**
   * Logging for error messages.
   */
  Logging logger = Logging.getLogger(ReusingDBIDFactory.class);
  
  /**
   * Bit set to keep track of dynamic DBIDs
   */
  BitSet dynamicUsed = new BitSet();
  
  /**
   * Keep track of the lowest unused dynamic DBID
   */
  int dynamicStart = 0;

  // TODO: add an offset, to save keeping long bit sets of 1s for heavy dynamic use?

  /**
   * Returned range allocations
   */
  ArrayList<DBIDRangeAllocation> returnedAllocations = new ArrayList<DBIDRangeAllocation>();

  /**
   * Constructor
   */
  public ReusingDBIDFactory() {
    super();
  }

  @Override
  public synchronized DBID generateSingleDBID() {
    dynamicStart = dynamicUsed.nextClearBit(dynamicStart);
    dynamicUsed.set(dynamicStart);
    return new DBID(-(dynamicStart + 1));
  }

  @Override
  public synchronized void deallocateSingleDBID(DBID id) {
    if (id.getIntegerID() >= 0) {
      logger.warning("Single DBID returned is from a range allocation!");
      return;
    }
    int pos = - id.getIntegerID() - 1;
    dynamicUsed.clear(pos);
    dynamicStart = Math.min(dynamicStart, pos);
  }

  @Override
  public synchronized DBIDRangeAllocation generateStaticDBIDRange(int size) {
    for (int i = 0; i < returnedAllocations.size(); i++) {
      DBIDRangeAllocation alloc = returnedAllocations.get(i);
      if (alloc.len == size) {
        returnedAllocations.remove(i);
        return alloc;
      }
    }
    for (int i = 0; i < returnedAllocations.size(); i++) {
      DBIDRangeAllocation alloc = returnedAllocations.get(i);
      if (alloc.len > size) {
        DBIDRangeAllocation retalloc = new DBIDRangeAllocation(alloc.start, size);
        alloc = new DBIDRangeAllocation(alloc.start + size, alloc.len - size);
        returnedAllocations.set(i, alloc);
        return retalloc;
      }
    }
    return super.generateStaticDBIDRange(size);
  }

  @Override
  public synchronized void deallocateDBIDRange(DBIDRangeAllocation range) {
    returnedAllocations.add(range);
  }
}