package de.lmu.ifi.dbs.elki.utilities.datastructures;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

/**
 * Test the in-memory bounded heap class.
 * 
 * @author Erich Schubert
 */
public class TestTiedTopBoundedHeap {
  /**
   * Test bounded heap
   */
  @Test
  public void testTiedTopBoundedHeap() {
    Integer[] data = { 5, 3, 4, 2, 7, 1, 9, 8, 10, 6, 5 };
    Integer[] asc = { 5, 5, 6, 7, 8, 9, 10 };
    Integer[] desc = { 5, 5, 4, 3, 2, 1 };
    Heap<Integer> hasc = new TiedTopBoundedHeap<Integer>(asc.length - 1);
    Heap<Integer> hdesc = new TiedTopBoundedHeap<Integer>(desc.length - 1, Collections.reverseOrder());
    for(Integer i : data) {
      hasc.add(i);
      hdesc.add(i);
    }
    //LoggingUtil.warning("Heap: "+hasc.toString()+ " -- "+hdesc.toString());
    assertEquals("Ascending heap size doesn't match", asc.length, hasc.size());
    assertEquals("Descending heap size doesn't match", desc.length, hdesc.size());
    for(int i = 0; i < asc.length; i++) {
      final Integer gota = hasc.poll();
      assertEquals("Objects sorted incorrectly at ascending position "+i, asc[i], gota);
    }
    for(int i = 0; i < desc.length; i++) {
      final Integer gotd = hdesc.poll();
      assertEquals("Objects sorted incorrectly at descending position "+i, desc[i], gotd);
    }
  }
  /**
   * Test bounded heap
   */
  @Test
  public void testTiedTopBoundedHeapTrival() {
    Heap<Integer> heap1 = new TiedTopBoundedHeap<Integer>(1);
    Heap<Integer> heap2 = new TiedTopBoundedHeap<Integer>(1);
    Heap<Integer> heap3 = new TiedTopBoundedHeap<Integer>(1);
    Heap<Integer> heap4 = new TiedTopBoundedHeap<Integer>(1);
    Heap<Integer> heap5 = new TiedTopBoundedHeap<Integer>(1);
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
