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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.junit.Test;

/**
 * Test the updateable heap used by OPTICS.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class UpdatableHeapTest {
  @Test
  public void testUpdatableHeap() {
    final int iters = 100;
    final int maxid = 5000;
    final int bsize = 100;
    final Random r = new Random(1);
    ArrayList<Entry> simulate = new ArrayList<>(1000);
    UpdatableHeap<Entry> heap = new UpdatableHeap<>();
    for(int i = 0; i < iters; i++) {
      int batchsize = r.nextInt(bsize);
      for(int j = 0; j < batchsize; j++) {
        int id = r.nextInt(maxid);
        int score = r.nextInt(10000);
        // Update heap
        heap.add(new Entry(score, id));
        // Update simulation
        boolean found = false;
        for(Entry ent : simulate) {
          if(ent.key == id) {
            if(score > ent.priority) {
              // System.err.println("Updating in ArrayList: " + ent + " to " +
              // score);
              ent.priority = score;
            }
            found = true;
            break;
          }
        }
        if(!found) {
          simulate.add(new Entry(score, id));
        }
      }
      // Keeping the simulation list reverse is a bit faster for removal
      Collections.sort(simulate, Collections.reverseOrder());
      // System.err.println(simulate.size() + " <=> " + heap.size());
      assertEquals("Sizes don't match!", simulate.size(), heap.size());
      int remove = r.nextInt(simulate.size());
      for(int j = 0; j < remove; j++) {
        // System.out.println(heap.toString());
        Entry fromheap = heap.poll();
        Entry fromsim = simulate.remove(simulate.size() - 1);
        // System.out.println(fromheap+" <=> "+fromsim);
        assertEquals("Priority doesn't agree.", fromheap.priority, fromsim.priority);
        // Careful with ties!
        if(fromheap.key != fromsim.key && j + 1 == remove && heap.size() > 0) {
          j++;
        }
      }
    }
  }

  /**
   * Heap entry.
   */
  static class Entry implements Comparable<Entry> {
    /**
     * Priority
     */
    int priority;

    /**
     * Key
     */
    int key;

    /**
     * Constructor.
     *
     * @param priority Priority
     * @param key Key
     */
    public Entry(int priority, int key) {
      this.priority = priority;
      this.key = key;
    }

    @Override
    public int hashCode() {
      return key;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Entry && key == ((Entry) obj).key;
    }

    @Override
    public int compareTo(Entry o) {
      return Integer.compare(o.priority, this.priority);
    }
  }
}
