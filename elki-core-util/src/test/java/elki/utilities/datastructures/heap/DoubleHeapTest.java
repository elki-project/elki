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
package elki.utilities.datastructures.heap;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

/**
 * Perform standard unit tests on the double-indexed heap structures.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class DoubleHeapTest {
  @Test
  public void testDoubleMinHeap() {
    Random r = new Random();
    DoubleMinHeap heap = new DoubleMinHeap();
    for (int i = 0; i < 1000; i++) {
      double key = r.nextDouble();
      heap.add(key);
    }
    double cur = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < 500; i++) {
      assertTrue("Heap incorrectly ordered!", cur <= heap.peek());
      cur = heap.peek();
      heap.poll();
    }
    for (int i = 0; i < 10000; i++) {
      double key = r.nextDouble();
      heap.add(key);
    }
    cur = Double.NEGATIVE_INFINITY;
    while (heap.size() > 0) {
      assertTrue("Heap incorrectly ordered!", cur <= heap.peek());
      cur = heap.peek();
      heap.poll();
    }
  }

  @Test
  public void testDoubleMaxHeap() {
    Random r = new Random();
    DoubleMaxHeap heap = new DoubleMaxHeap();
    for (int i = 0; i < 1000; i++) {
      double key = r.nextDouble();
      heap.add(key);
    }
    double cur = Double.POSITIVE_INFINITY;
    for (int i = 0; i < 500; i++) {
      assertTrue("Heap incorrectly ordered! "+cur+" < "+heap.peek(), cur >= heap.peek());
      cur = heap.peek();
      heap.poll();
    }
    for (int i = 0; i < 10000; i++) {
      double key = r.nextDouble();
      heap.add(key);
    }
    cur = Double.POSITIVE_INFINITY;
    while (heap.size() > 0) {
      assertTrue("Heap incorrectly ordered!", cur >= heap.peek());
      cur = heap.peek();
      heap.poll();
    }
  }
}
