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
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

/**
 * Test the in-memory heap class.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class HeapTest {
  /**
   * Puts 10 integers into both an ascending and a descending heap and verifies
   * they come out in sequence.
   */
  @Test
  public void testHeap() {
    int dup = 2;
    Integer[] data = { 5, 3, 4, 2, 7, 1, 9, 8, 10, 6 };
    Integer[] asc = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    Integer[] desc = { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
    ComparableMinHeap<Integer> hasc = new ComparableMinHeap<>();
    ComparableMaxHeap<Integer> hdesc = new ComparableMaxHeap<>();
    for(Integer i : data) {
      for(int j = 0; j < dup; j++) {
        hasc.add(i);
        hdesc.add(i);
      }
    }
    // Empty
    for(int i = 0; i < asc.length; i++) {
      for(int j = 0; j < dup; j++) {
        final Integer gota = hasc.poll();
        assertEquals("Objects sorted incorrectly at ascending position " + i, asc[i], gota);
        final Integer gotd = hdesc.poll();
        assertEquals("Objects sorted incorrectly at descending position " + i, desc[i], gotd);
      }
    }
    // Refill
    for(Integer i : data) {
      for(int j = 0; j < dup; j++) {
        hasc.add(i);
        hdesc.add(i);
      }
    }
    // Empty halfway
    for(int i = 0; i < 5; i++) {
      for(int j = 0; j < dup; j++) {
        final Integer gota = hasc.poll();
        assertEquals("Objects sorted incorrectly at ascending position " + i, asc[i], gota);
        final Integer gotd = hdesc.poll();
        assertEquals("Objects sorted incorrectly at descending position " + i, desc[i], gotd);
      }
    }
    // Re-add
    for(int i = 0; i < 5; i++) {
      for(int j = 0; j < dup; j++) {
        hasc.add(asc[i]);
        hdesc.add(desc[i]);
      }
    }
    // Empty again
    for(int i = 0; i < asc.length; i++) {
      for(int j = 0; j < dup; j++) {
        final Integer gota = hasc.poll();
        assertEquals("Objects sorted incorrectly at ascending position " + i, asc[i], gota);
        final Integer gotd = hdesc.poll();
        assertEquals("Objects sorted incorrectly at descending position " + i, desc[i], gotd);
      }
    }
    // Sequential insert
    for(int i : asc) {
      for(int j = 0; j < dup; j++) {
        hasc.add(i);
        hdesc.add(i);
      }
    }
    // Empty halfway
    for(int i = 0; i < 5; i++) {
      for(int j = 0; j < dup; j++) {
        final Integer gota = hasc.poll();
        assertEquals("Objects sorted incorrectly at ascending position " + i, asc[i], gota);
        final Integer gotd = hdesc.poll();
        assertEquals("Objects sorted incorrectly at descending position " + i, desc[i], gotd);
      }
    }
    // Re-add
    for(int i = 0; i < 5; i++) {
      for(int j = 0; j < dup; j++) {
        hasc.add(asc[i]);
        hdesc.add(desc[i]);
      }
    }
    // Empty again
    for(int i = 0; i < asc.length; i++) {
      for(int j = 0; j < dup; j++) {
        final Integer gota = hasc.poll();
        assertEquals("Objects sorted incorrectly at ascending position " + i, asc[i], gota);
        final Integer gotd = hdesc.poll();
        assertEquals("Objects sorted incorrectly at descending position " + i, desc[i], gotd);
      }
    }
    
    // Bonus: Sequential insert lower part only
    for(int i = 0; i < asc.length >> 1; i++) {
      for(int j = 0; j < dup << 1; j++) {
        hasc.add(asc[i]);
        hdesc.add(desc[i]);
      }
    }
    // Empty halfway
    for(int i = 0; i < 3; i++) {
      for(int j = 0; j < dup << 1; j++) {
        final Integer gota = hasc.poll();
        assertEquals("Objects sorted incorrectly at ascending position " + i, asc[i], gota);
        final Integer gotd = hdesc.poll();
        assertEquals("Objects sorted incorrectly at descending position " + i, desc[i], gotd);
      }
    }
    // Add upper half
    for(int i = asc.length >> 1; i < asc.length; i++) {
      for(int j = 0; j < dup; j++) {
        hasc.add(asc[i]);
        hdesc.add(desc[i]);
      }
    }
    //System.err.println(hasc.toString() + " " + hasc.validSize);
    // Empty again
    for(int i = 3; i < asc.length; i++) {
      int f = (i < (asc.length >> 1)) ? 2 : 1;
      for(int j = 0; j < dup * f; j++) {
        final Integer gota = hasc.poll();
        //System.err.println(hasc.toString() + " " + hasc.validSize);
        assertEquals("Objects sorted incorrectly at ascending position " + i, asc[i], gota);
        final Integer gotd = hdesc.poll();
        assertEquals("Objects sorted incorrectly at descending position " + i, desc[i], gotd);
      }
    }
  }

  /**
   * Puts 10 integers into both an ascending and a descending heap and verifies
   * they come out in sequence.
   */
  @Test
  public void testHeapRandomInt() {
    int size = 10000;
    Random r = new Random(123L);
    ComparableMinHeap<Integer> hasc = new ComparableMinHeap<>();
    ComparableMaxHeap<Integer> hdesc = new ComparableMaxHeap<>();
    for(int i = 0; i < size; i++) {
      int in = r.nextInt();
      hasc.add(in);
      hdesc.add(in);
    }
    int last = Integer.MIN_VALUE;
    for(int i = 0; i < size; i++) {
      final Integer gota = hasc.poll();
      assertTrue("Objects sorted incorrectly at ascending position " + i, gota >= last);
      last = gota;
    }
    last = Integer.MAX_VALUE;
    for(int i = 0; i < size; i++) {
      final Integer gotd = hdesc.poll();
      assertTrue("Objects sorted incorrectly at descending position " + i, gotd <= last);
      last = gotd;
    }
    // Rerun, but only halfway down
    for(int i = 0; i < size; i++) {
      int in = r.nextInt();
      hasc.add(in);
      hdesc.add(in);
    }
    last = Integer.MIN_VALUE;
    for(int i = 0; i < size >>> 1; i++) {
      final Integer gota = hasc.poll();
      assertTrue("Objects sorted incorrectly at ascending position " + i, gota >= last);
      last = gota;
    }
    last = Integer.MAX_VALUE;
    for(int i = 0; i < size >>> 1; i++) {
      final Integer gotd = hdesc.poll();
      assertTrue("Objects sorted incorrectly at descending position " + i, gotd <= last);
      last = gotd;
    }
    // Refill:
    for(int i = size >>> 1; i < size; i++) {
      int in = r.nextInt();
      hasc.add(in);
      hdesc.add(in);
    }
    last = Integer.MIN_VALUE;
    for(int i = 0; i < size; i++) {
      final Integer gota = hasc.poll();
      assertTrue("Objects sorted incorrectly at ascending position " + i, gota >= last);
      last = gota;
    }
    last = Integer.MAX_VALUE;
    for(int i = 0; i < size; i++) {
      final Integer gotd = hdesc.poll();
      assertTrue("Objects sorted incorrectly at descending position " + i, gotd <= last);
      last = gotd;
    }
  }
}