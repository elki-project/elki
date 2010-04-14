package de.lmu.ifi.dbs.elki.utilities.datastructures;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

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
