package de.lmu.ifi.dbs.elki.database.ids.integer;

import java.util.ArrayList;
import java.util.BitSet;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.RangeDBIDs;
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
  private static final Logging logger = Logging.getLogger(ReusingDBIDFactory.class);
  
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
  ArrayList<IntegerDBIDRange> returnedAllocations = new ArrayList<IntegerDBIDRange>();

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
    return DBIDFactory.FACTORY.importInteger(-(dynamicStart + 1));
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
  public synchronized RangeDBIDs generateStaticDBIDRange(int size) {
    for (int i = 0; i < returnedAllocations.size(); i++) {
      IntegerDBIDRange alloc = returnedAllocations.get(i);
      if (alloc.size() == size) {
        returnedAllocations.remove(i);
        return alloc;
      }
    }
    for (int i = 0; i < returnedAllocations.size(); i++) {
      IntegerDBIDRange alloc = returnedAllocations.get(i);
      if (alloc.size() > size) {
        IntegerDBIDRange retalloc = new IntegerDBIDRange(alloc.start, size);
        alloc = new IntegerDBIDRange(alloc.start + size, alloc.size() - size);
        returnedAllocations.set(i, alloc);
        return retalloc;
      }
    }
    return super.generateStaticDBIDRange(size);
  }

  @Override
  public synchronized void deallocateDBIDRange(RangeDBIDs range) {
    // TODO: catch an eventual cast exception?
    returnedAllocations.add((IntegerDBIDRange)range);
  }
}