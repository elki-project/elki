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
package de.lmu.ifi.dbs.elki.utilities.datastructures.unionfind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;

/**
 * Unit test for union find.
 *
 * @author Evgeniy Faerman
 * @author Erich Schubert
 * @since 0.7.0
 */
public class WeightedQuickUnionStaticDBIDsTest {
  @Test
  public void testTree() {
    DBIDRange range = DBIDUtil.generateStaticDBIDRange(8);
    UnionFind uf = new WeightedQuickUnionStaticDBIDs(range);
    DBIDArrayIter i1 = range.iter(), i2 = range.iter();
    assertFalse(uf.isConnected(i1.seek(0), i2.seek(7)));
    uf.union(i1.seek(0), i2.seek(1));
    assertTrue(uf.isConnected(i1.seek(0), i2.seek(1)));
    uf.union(i1.seek(2), i2.seek(3));
    assertFalse(uf.isConnected(i1.seek(0), i2.seek(2)));
    uf.union(i1.seek(0), i2.seek(2));
    assertTrue(uf.isConnected(i1.seek(3), i2.seek(1)));
    uf.union(i1.seek(4), i2.seek(5));
    uf.union(i1.seek(6), i2.seek(7));
    uf.union(i1.seek(4), i2.seek(6));
    assertFalse(uf.isConnected(i1.seek(0), i2.seek(4)));
    uf.union(i1.seek(0), i2.seek(4));
    for(int i = 0; i < 8; i++) {
      for(int j = 0; j < 8; j++) {
        assertTrue(uf.isConnected(i1.seek(i), i2.seek(j)));
      }
    }
  }

  @Test
  public void testBruteForce() {
    final Random r = new Random(0L);
    final int size = 100;
    DBIDRange range = DBIDUtil.generateStaticDBIDRange(size);

    UnionFind uf = new WeightedQuickUnionStaticDBIDs(range);
    DBIDArrayIter i1 = range.iter(), i2 = range.iter();

    int[] c = new int[size];
    for(int i = 0; i < size; i++) {
      c[i] = i;
    }
    int numc = size;
    while(numc > 1) {
      // Two randoms, with o1 < o2
      int o2 = r.nextInt(size - 1) + 1, o1 = r.nextInt(o2);
      final int c1 = c[o1], c2 = c[o2];
      final boolean ufc = uf.isConnected(i1.seek(o1), i2.seek(o2));
      assertEquals(c1 == c2, ufc);
      uf.union(i1, i2); // always
      if(c1 != c2) {
        for(int j = 0; j < size; j++) {
          if(c[j] == c1) {
            c[j] = c2;
          }
        }
        --numc;
      }
      assertEquals(numc, uf.getRoots().size());
    }
  }

  /**
   * Worst-case with 10 nodes, from Sedgewick.
   *
   * We don't test runtime, but this is an interesting case nevertheless.
   */
  @Test
  public void testWorstCase() {
    DBIDRange range = DBIDUtil.generateStaticDBIDRange(10);
    UnionFind uf = new WeightedQuickUnionStaticDBIDs(range);
    DBIDArrayIter i1 = range.iter(), i2 = range.iter();
    assertFalse(uf.isConnected(i1.seek(0), i2.seek(1)));
    uf.union(i1.seek(0), i2.seek(1));
    assertTrue(uf.isConnected(i1.seek(0), i2.seek(1)));
    uf.union(i1.seek(2), i2.seek(3));
    assertFalse(uf.isConnected(i1.seek(0), i2.seek(2)));
    uf.union(i1.seek(5), i2.seek(4));
    uf.union(i1.seek(7), i2.seek(6));
    uf.union(i1.seek(8), i2.seek(9));
    uf.union(i1.seek(1), i2.seek(3));
    assertTrue(uf.isConnected(i1.seek(0), i2.seek(2)));
    uf.union(i1.seek(4), i2.seek(6));
    assertTrue(uf.isConnected(i1.seek(5), i2.seek(7)));
    uf.union(i1.seek(3), i2.seek(7));
    assertTrue(uf.isConnected(i1.seek(0), i2.seek(4)));
    assertFalse(uf.isConnected(i1.seek(0), i2.seek(9)));
    uf.union(i1.seek(0), i2.seek(9));
    for(int i = 0; i < 8; i++) {
      for(int j = 0; j < 8; j++) {
        assertTrue(uf.isConnected(i1.seek(i), i2.seek(j)));
      }
    }
  }

  @Test
  public void testRoots() {
    DBIDRange range = DBIDUtil.generateStaticDBIDRange(8);
    UnionFind uf = new WeightedQuickUnionStaticDBIDs(range);
    DBIDArrayIter i1 = range.iter(), i2 = range.iter();
    uf.union(i1.seek(0), i2.seek(1));
    uf.union(i1.seek(2), i2.seek(3));
    assertEquals(6, uf.getRoots().size());
    uf.union(i1.seek(0), i2.seek(2));
    assertEquals(5, uf.getRoots().size());
    uf.union(i1.seek(4), i2.seek(5));
    uf.union(i1.seek(6), i2.seek(7));
    uf.union(i1.seek(4), i2.seek(6));
    assertEquals(2, uf.getRoots().size());
    uf.union(i1.seek(0), i2.seek(4));
    assertEquals(1, uf.getRoots().size());
  }
}
