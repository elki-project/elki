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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

/**
 * Test for min and max heaps.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class DoubleIntegerDBIDHeapTest {
  @Test
  public void testHeaps() {
    double[] dists = { 1., 2., 4., 8., 4., 6., 2., 0., 6., 8., };
    double[] sorted = dists.clone();
    Arrays.sort(sorted);
    DBIDRange range = DBIDFactory.FACTORY.generateStaticDBIDRange(dists.length);
    DoubleDBIDHeap heap = DBIDUtil.newMinHeap(5);
    for(DBIDArrayIter it = range.iter(); it.valid(); it.advance()) {
      heap.insert(dists[it.getOffset()], it, 5);
    }
    assertEquals("heap size bad", 5, heap.size());
    for(int i = 0, j = sorted.length - 5; i < 5; i++, j++) {
      assertEquals(i + " largest wrong", sorted[j], heap.peekKey(), 0.);
      heap.poll();
    }
    heap = DBIDUtil.newMaxHeap(5);
    for(DBIDArrayIter it = range.iter(); it.valid(); it.advance()) {
      heap.insert(dists[it.getOffset()], it, 5);
    }
    assertEquals("heap size bad", 5, heap.size());
    for(int i = 4; i >= 0; i--) {
      assertEquals(i + " smallest wrong", sorted[i], heap.peekKey(), 0.);
      heap.poll();
    }
  }
}
