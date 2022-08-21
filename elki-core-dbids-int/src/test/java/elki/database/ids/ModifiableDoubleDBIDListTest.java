/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.database.ids;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test a corner case when sorting: duplicate keys.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ModifiableDoubleDBIDListTest {
  // Needs to be outside of the timeout below
  // Because this may involve scanning for DBIDFactory implementations
  private static final DBID id = DBIDUtil.importInteger(1);

  @Test(timeout = 100)
  public void testDuplicateKeys() {
    // We need an id, but no real data.
    int size = 100000;

    ModifiableDoubleDBIDList list = DBIDUtil.newDistanceDBIDList(size);
    for(int i = 0; i < size; i++) {
      list.add(i & 0x1, id);
    }
    list.sort();
    double last = Double.NEGATIVE_INFINITY;
    for(DoubleDBIDListIter it = list.iter(); it.valid(); it.advance()) {
      assertTrue("Not sorted", last <= it.doubleValue());
      last = it.doubleValue();
    }
  }
}
