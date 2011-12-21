package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

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
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;

public class TestTiedTopBoundedUpdatableHeap implements JUnit4Test {
  @Test
  public void testTiedTopBoundedUpdatableHeap() {
    final int iters = 100;
    final int maxid = 5000;
    final int bsize = 1000;
    final int limit = 200;
    final Random r = new Random(1);
    ArrayList<IntegerPriorityObject<Integer>> simulate = new ArrayList<IntegerPriorityObject<Integer>>(1000);
    UpdatableHeap<IntegerPriorityObject<Integer>> heap = new TiedTopBoundedUpdatableHeap<IntegerPriorityObject<Integer>>(limit);
    for(int i = 0; i < iters; i++) {
      int batchsize = r.nextInt(bsize);
      for(int j = 0; j < batchsize; j++) {
        int id = r.nextInt(maxid);
        int score = r.nextInt(10000);
        // Update heap
        heap.add(new IntegerPriorityObject<Integer>(score, id));
        // Update simulation
        boolean found = false;
        for(IntegerPriorityObject<Integer> ent : simulate) {
          if(ent.getObject().equals(id)) {
            if(score > ent.priority) {
              ent.priority = score;
            }
            found = true;
            break;
          }
        }
        if(!found) {
          simulate.add(new IntegerPriorityObject<Integer>(score, id));
        }
        Collections.sort(simulate, Collections.reverseOrder());
        while(simulate.size() > limit) {
          int max = simulate.get(limit - 1).priority;
          if(simulate.get(simulate.size() - 1).priority == max) {
            break;
          }
          else {
            // assertTrue(simulate.get(simulate.size() - 1).priority > max);
            simulate.remove(simulate.size() - 1);
          }
        }
        if(simulate.size() != heap.size()) {
          System.err.println("Lost synchronization: " + id + " to " + score);
          System.err.println("Sim: " + simulate.get(simulate.size() - 1) + " <=> Heap: " + heap.peek());
        }
        assertEquals("Sizes don't match!", simulate.size(), heap.size());
      }
      // System.err.println("Sim: " + simulate.get(simulate.size() - 1) + " <=> Heap: " + heap.peek());
      // System.err.println(simulate.size() + " <=> " + heap.size());
      int remove = r.nextInt(simulate.size());
      for(int j = 0; j < remove; j++) {
        // System.out.println(heap.toString());
        IntegerPriorityObject<Integer> fromheap = heap.poll();
        IntegerPriorityObject<Integer> fromsim = simulate.remove(simulate.size() - 1);
        // System.out.println(fromheap+" <=> "+fromsim);
        assertEquals("Priority doesn't agree.", fromheap.priority, fromsim.priority);
        if(j + 1 == remove) {
          while(heap.size() > 0) {
            assertEquals("Priority doesn't agree.", heap.peek().priority, simulate.get(simulate.size() - 1).priority);
            if(heap.peek().priority == fromheap.priority) {
              fromheap = heap.poll();
              fromsim = simulate.remove(simulate.size() - 1);
            }
            else {
              break;
            }
          }
        }
      }
    }
  }
}
