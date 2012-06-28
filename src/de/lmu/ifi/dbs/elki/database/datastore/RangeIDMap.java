package de.lmu.ifi.dbs.elki.database.datastore;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * Mapping a static DBID range to storage IDs.
 * 
 * @author Erich Schubert
 */
public class RangeIDMap implements DataStoreIDMap {
  /**
   * Start offset
   */
  final DBIDRange range;
  
  /**
   * Constructor from a static DBID range allocation.
   * 
   * @param range DBID range to use
   */
  public RangeIDMap(DBIDRange range) {
    this.range = range;
  }

  @Override
  public int map(DBIDRef dbid) {
    return range.getOffset(dbid);
  }
}
