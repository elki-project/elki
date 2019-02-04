/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.database.ids.integer;

import java.util.ArrayList;
import java.util.BitSet;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Slightly more complex DBID management, that allows reuse of DBIDs.
 * 
 * NOT tested a lot yet. Not reusing is much simpler!
 * 
 * TODO: manage fragmentation of ranges?
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @stereotype factory
 */
public class ReusingDBIDFactory extends SimpleDBIDFactory {
  /**
   * Logging for error messages.
   */
  private static final Logging LOG = Logging.getLogger(ReusingDBIDFactory.class);

  /**
   * Bit set to keep track of dynamic DBIDs
   */
  BitSet dynamicUsed = new BitSet();

  /**
   * Keep track of the lowest unused dynamic DBID
   */
  int dynamicStart = 0;

  // TODO: add an offset, to save keeping long bit sets of 1s for heavy dynamic
  // use?

  /**
   * Returned range allocations
   */
  ArrayList<IntegerDBIDRange> returnedAllocations = new ArrayList<>();

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
  public synchronized void deallocateSingleDBID(DBIDRef id) {
    final int intid = id.internalGetIndex();
    if (intid >= 0) {
      LOG.warning("Single DBID returned is from a range allocation!");
      return;
    }
    final int pos = -intid - 1;
    dynamicUsed.clear(pos);
    dynamicStart = Math.min(dynamicStart, pos);
  }

  @Override
  public synchronized DBIDRange generateStaticDBIDRange(int size) {
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
  public synchronized void deallocateDBIDRange(DBIDRange range) {
    returnedAllocations.add((IntegerDBIDRange) range);
  }
}
