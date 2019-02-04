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
package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

/**
 * Test the in-memory bounded heap class.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class TiedTopBoundedHeapTest {
  /**
   * Test bounded heap
   */
  @Test
  public void testTiedTopBoundedHeap() {
    Integer[] data = { 5, 3, 4, 2, 7, 1, 9, 8, 10, 6, 5 };
    Integer[] asc = { 5, 5, 6, 7, 8, 9, 10 };
    Integer[] desc = { 5, 5, 4, 3, 2, 1 };
    Heap<Integer> hasc = new TiedTopBoundedHeap<>(asc.length - 1);
    Heap<Integer> hdesc = new TiedTopBoundedHeap<>(desc.length - 1, Collections.reverseOrder());
    for(Integer i : data) {
      hasc.add(i);
      hdesc.add(i);
    }
    // LoggingUtil.warning("Heap: "+hasc.toString()+ " -- "+hdesc.toString());
    assertEquals("Ascending heap size doesn't match", asc.length, hasc.size());
    assertEquals("Descending heap size doesn't match", desc.length, hdesc.size());
    for(int i = 0; i < asc.length; i++) {
      final Integer gota = hasc.poll();
      assertEquals("Objects sorted incorrectly at ascending position " + i, asc[i], gota);
    }
    for(int i = 0; i < desc.length; i++) {
      final Integer gotd = hdesc.poll();
      assertEquals("Objects sorted incorrectly at descending position " + i, desc[i], gotd);
    }
  }

  /**
   * Test bounded heap
   */
  @Test
  public void testTiedTopBoundedHeapTrival() {
    Heap<Integer> heap1 = new TiedTopBoundedHeap<>(1);
    Heap<Integer> heap2 = new TiedTopBoundedHeap<>(1);
    Heap<Integer> heap3 = new TiedTopBoundedHeap<>(1);
    Heap<Integer> heap4 = new TiedTopBoundedHeap<>(1);
    Heap<Integer> heap5 = new TiedTopBoundedHeap<>(1);
    heap2.add(2);
    heap4.add(0);
    for(int i = 0; i < 10; i++) {
      heap1.add(1);
      heap2.add(1);
      heap3.add(1);
      heap4.add(1);
      heap5.add(1);
    }
    heap3.add(2);
    heap5.add(0);
    assertEquals("First heap size doesn't match", 10, heap1.size());
    assertEquals("Second heap size doesn't match", 1, heap2.size());
    assertEquals("Third heap size doesn't match", 1, heap3.size());
    assertEquals("Fourth heap size doesn't match", 10, heap4.size());
    assertEquals("Fifth heap size doesn't match", 10, heap5.size());
  }
}
