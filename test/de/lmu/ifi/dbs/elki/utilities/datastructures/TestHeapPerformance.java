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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;

public class TestHeapPerformance implements JUnit4Test {
  final private int queueSize = 100000;
  
  final private int iterations = 1;

  final private long seed = 123456L;

  @Test
  public void testRuntime() throws Exception {
    // prepare the data set
    final List<Integer> elements = new ArrayList<Integer>(queueSize);
    {
      final Random random = new Random(seed);
      for(int i = 0; i < queueSize; i++) {
        elements.add(i);
      }
      Collections.shuffle(elements, random);
    }

    long hstart = System.currentTimeMillis();
    {
      for(int j = 0; j < iterations; j++) {
        Heap<Integer> pq = new Heap<Integer>(); //Collections.reverseOrder());
        // Insert all
        for(int i = 0; i < elements.size(); i++) {
          pq.add(elements.get(i));
        }
        // Poll first half.
        for(int i = 0; i < elements.size() >> 1; i++) {
          assertEquals((int) pq.poll(), i);
          // assertEquals((int) pq.poll(), queueSize - 1 - i);
        }
      }
    }
    long htime = System.currentTimeMillis() - hstart;

    long pqstart = System.currentTimeMillis();
    {
      for(int j = 0; j < iterations; j++) {
        PriorityQueue<Integer> pq = new PriorityQueue<Integer>(); //11, Collections.reverseOrder());
        // Insert all
        for(int i = 0; i < elements.size(); i++) {
          pq.add(elements.get(i));
        }
        // Poll first half.
        for(int i = 0; i < elements.size() >> 1; i++) {
          assertEquals((int) pq.poll(), i);
          // assertEquals((int) pq.poll(), queueSize - 1 - i);
        }
      }
    }
    long pqtime = System.currentTimeMillis() - pqstart;
    System.err.println(pqtime + " > " + htime);
  }
}