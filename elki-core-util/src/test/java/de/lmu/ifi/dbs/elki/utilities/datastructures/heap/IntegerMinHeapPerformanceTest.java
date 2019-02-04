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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit test to ensure that our heap is not significantly worse than SUN javas
 * regular PriorityQueue.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class IntegerMinHeapPerformanceTest {
  final private int queueSize = 100000;

  final private int iterations = 20;

  final private long seed = 123456L;

  @Ignore
  @Test
  public void testRuntime() throws Exception {
    // prepare the data set
    final List<Integer> elements = new ArrayList<>(queueSize);
    {
      final Random random = new Random(seed);
      for(int i = 0; i < queueSize; i++) {
        elements.add(i);
      }
      Collections.shuffle(elements, random);
    }

    // Pretest, to trigger hotspot compiler, hopefully.
    {
      for(int j = 0; j < iterations; j++) {
        IntegerMinHeap pq = new IntegerMinHeap();
        testHeap(elements, pq);
      }
      for(int j = 0; j < iterations; j++) {
        PriorityQueue<Integer> pq = new PriorityQueue<>(); // 11,
        testQueue(elements, pq);
      }
    }

    long hstart = System.nanoTime();
    {
      for(int j = 0; j < iterations; j++) {
        IntegerMinHeap pq = new IntegerMinHeap();
        testHeap(elements, pq);
      }
    }
    long htime = System.nanoTime() - hstart;

    long pqstart = System.nanoTime();
    {
      for(int j = 0; j < iterations; j++) {
        PriorityQueue<Integer> pq = new PriorityQueue<>(); // 11
        testQueue(elements, pq);
      }
    }
    long pqtime = System.nanoTime() - pqstart;
    // System.err.println("Heap performance test: us: " + htime*1E-9 + " java: " + pqtime*1E-9);
    assertTrue("Heap performance regression - run test individually, since the hotspot optimizations may make the difference! " + htime + " >>= " + pqtime, htime < 1.05 * pqtime);
    // 1.05 allows some difference in measuring
  }

  private void testHeap(final List<Integer> elements, IntegerMinHeap pq) {
    // Insert all
    for(int i = 0; i < elements.size(); i++) {
      pq.add(elements.get(i));
    }
    // Poll first half.
    final int half = elements.size() >> 1;
    for(int i = 0; i < half; i++) {
      assertEquals(pq.poll(), i);
      // assertEquals((int) pq.poll(), queueSize - 1 - i);
    }
    assertEquals("Heap not half-empty?", elements.size() - half, pq.size());
    pq.clear();
  }

  private void testQueue(final List<Integer> elements, Queue<Integer> pq) {
    // Insert all
    for(int i = 0; i < elements.size(); i++) {
      pq.add(elements.get(i));
    }
    // Poll first half.
    final int half = elements.size() >> 1;
    for(int i = 0; i < half; i++) {
      assertEquals((int) pq.poll(), i);
      // assertEquals((int) pq.poll(), queueSize - 1 - i);
    }
    assertEquals("Heap not half-empty?", elements.size() - half, pq.size());
    pq.clear();
  }
}