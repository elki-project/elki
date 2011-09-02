package de.lmu.ifi.dbs.elki.utilities.datastructures;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TopBoundedHeap;

/**
 * Test the in-memory bounded heap class.
 * 
 * @author Erich Schubert
 */
public class TestTopBoundedHeap {
  /**
   * Test bounded heap
   */
  @Test
  public void testBoundedHeap() {
    Integer[] data = { 5, 3, 4, 2, 7, 1, 9, 8, 10, 6 };
    Integer[] asc = { 5, 6, 7, 8, 9, 10 };
    Integer[] desc = { 6, 5, 4, 3, 2, 1 };
    Heap<Integer> hasc = new TopBoundedHeap<Integer>(asc.length);
    Heap<Integer> hdesc = new TopBoundedHeap<Integer>(desc.length, Collections.reverseOrder());
    for(Integer i : data) {
      hasc.add(i);
      hdesc.add(i);
    }
    for(int i = 0; i < asc.length; i++) {
      final Integer gota = hasc.poll();
      assertEquals("Objects sorted incorrectly at ascending position "+i, asc[i], gota);
    }
    for(int i = 0; i < desc.length; i++) {
      final Integer gotd = hdesc.poll();
      assertEquals("Objects sorted incorrectly at descending position "+i, desc[i], gotd);
    }
  }
}
