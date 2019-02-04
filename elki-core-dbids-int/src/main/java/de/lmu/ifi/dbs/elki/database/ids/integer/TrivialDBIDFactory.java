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

import java.util.concurrent.atomic.AtomicInteger;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Trivial DBID management, that never reuses IDs and just gives them out in
 * sequence. All IDs will be positive.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @opt nodefillcolor LemonChiffon
 * @stereotype factory
 */
final public class TrivialDBIDFactory extends AbstractIntegerDBIDFactory {
  /**
   * Keep track of the smallest dynamic DBID offset not used.
   */
  AtomicInteger next = new AtomicInteger(1);

  /**
   * Constructor.
   */
  public TrivialDBIDFactory() {
    super();
  }

  @Override
  public DBID generateSingleDBID() {
    final int id = next.getAndIncrement();
    if(id == Integer.MAX_VALUE) {
      throw new AbortException("DBID allocation error - too many objects allocated!");
    }
    DBID ret = new IntegerDBID(id);
    return ret;
  }

  @Override
  public void deallocateSingleDBID(DBIDRef id) {
    // ignore for now
  }

  @Override
  public DBIDRange generateStaticDBIDRange(int size) {
    final int start = next.getAndAdd(size);
    if(start > next.get()) {
      throw new AbortException("DBID range allocation error - too many objects allocated!");
    }
    DBIDRange alloc = new IntegerDBIDRange(start, size);
    return alloc;
  }

  @Override
  public DBIDRange generateStaticDBIDRange(int begin, int size) {
    final int end = begin + size;
    if(end > Integer.MAX_VALUE) {
      throw new AbortException("DBID range allocation error - too many objects allocated!");
    }
    DBIDRange alloc = new IntegerDBIDRange(begin, size);
    int v;
    while((v = next.get()) < end) {
      if(next.compareAndSet(v, end)) {
        break;
      }
    }
    return alloc;
  }

  @Override
  public void deallocateDBIDRange(DBIDRange range) {
    // ignore.
  }
}
