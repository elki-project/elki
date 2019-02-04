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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Simple DBID management, that never reuses IDs. Statically allocated DBID
 * ranges are given positive values, Dynamically allocated DBIDs are given
 * negative values.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @opt nodefillcolor LemonChiffon
 * @stereotype factory
 */
public class SimpleDBIDFactory extends AbstractIntegerDBIDFactory {
  /**
   * Keep track of the smallest dynamic DBID offset not used.
   */
  int dynamicids = 0;

  /**
   * The starting point for static DBID range allocations.
   */
  int rangestart = 0;

  /**
   * Constructor.
   */
  public SimpleDBIDFactory() {
    super();
  }

  @Override
  public synchronized DBID generateSingleDBID() {
    dynamicids--;
    if(dynamicids == Integer.MIN_VALUE) {
      throw new AbortException("DBID range allocation error - too many objects allocated!");
    }
    return new IntegerDBID(dynamicids);
  }

  @Override
  public void deallocateSingleDBID(DBIDRef id) {
    // ignore for now.
  }

  @Override
  public synchronized DBIDRange generateStaticDBIDRange(int size) {
    if(rangestart >= Integer.MAX_VALUE - size) {
      throw new AbortException("DBID range allocation error - too many objects allocated!");
    }
    DBIDRange alloc = new IntegerDBIDRange(rangestart, size);
    rangestart += size;
    return alloc;
  }

  @Override
  public DBIDRange generateStaticDBIDRange(int begin, int size) {
    if(begin + size >= Integer.MAX_VALUE) {
      throw new AbortException("DBID range allocation error - too many objects allocated!");
    }
    DBIDRange alloc = new IntegerDBIDRange(begin, size);
    rangestart = Math.max(rangestart, begin + size);
    return alloc;
  }

  @Override
  public void deallocateDBIDRange(DBIDRange range) {
    // ignore.
  }
}