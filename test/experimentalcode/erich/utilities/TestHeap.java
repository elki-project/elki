package experimentalcode.erich.utilities;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;

/**
 * Test the in-memory heap class.
 * 
 * @author Erich Schubert
 */
public class TestHeap implements JUnit4Test {
  /**
   * Puts 10 integers into both an ascending and a descending heap and verifies
   * they come out in sequence.
   */
  @Test
  public void testHeap() {
    Integer[] data = { 5, 3, 4, 2, 7, 1, 9, 8, 10, 6 };
    Integer[] asc = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    Integer[] desc = { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
    Heap<Integer> hasc = new Heap<Integer>();
    Heap<Integer> hdesc = new Heap<Integer>(Collections.reverseOrder());
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
