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
package de.lmu.ifi.dbs.elki.database;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;

/**
 * Test a corner case when sorting: duplicate keys.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class SortingDuplicatesTest {
  @Test(timeout = 100)
  public void testDuplicateKeys() {
    // We need an ide, but no real data.
    DBID id = DBIDUtil.importInteger(1);
    int size = 100000;

    ModifiableDoubleDBIDList list = DBIDUtil.newDistanceDBIDList(size);
    for(int i = 0; i < size; i++) {
      double distance = 0. + (i % 2);
      list.add(distance, id);
    }
    list.sort();
  }
}
