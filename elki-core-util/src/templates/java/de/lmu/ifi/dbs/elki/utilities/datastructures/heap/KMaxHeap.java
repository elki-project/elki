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

import java.util.Arrays;

/**
 * Binary heap for primitive types.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @has - - - UnsortedIter
${genu ? " *\n * @param "+genu+" Key type\n" : ""} */
public class ${classname}${gend} implements ${boxed.equals("Comparable") ? "Object" : boxed}Heap${genu} {
  /**
   * Base heap.
   */
  protected ${raw}[] twoheap;

  /**
   * Current size of heap.
   */
  protected int size;

  /**
   * Initial size of the 2-ary heap.
   */
  private final static int TWO_HEAP_INITIAL_SIZE = (1 << 5) - 1;
${extra ? "\n  " + extra.fields +"\n" : ""}
  /**
   * Constructor, with default size.
   ${extra ? "*\n   * " + extra.param + "\n   " : ""}*/
  ${raw != type ? "@SuppressWarnings(\"unchecked\")\n  " : ""}public ${classname}(${extra ? extra.constructor : ""}) {
    super();${extra ? "\n    " + extra.init : ""}
    this.twoheap = new ${raw.replaceAll("<.*>", "")}[TWO_HEAP_INITIAL_SIZE];
  }

  /**
   * Constructor, with given minimum size.
   *
   * @param minsize Minimum size
   ${extra ? "* " + extra.param + "\n   " : ""}*/
  ${raw != type ? "@SuppressWarnings(\"unchecked\")\n  " : ""}public ${classname}(int minsize${extra ? ", " + extra.constructor : ""}) {
    super();${extra ? "\n    " + extra.init : ""}
    this.twoheap = new ${raw.replaceAll("<.*>", "")}[HeapUtil.nextPow2Int(minsize + 1) - 1];
  }

  @Override
  public void clear() {
    size = 0;
    Arrays.fill(twoheap, ${zero});
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  ${raw != type && raw != "Object" ? "@SuppressWarnings(\"unchecked\")\n  " : ""}public void add(${type} o) {
    final ${raw} co = ${raw != type && raw != "Object" ? "(" + raw + ") " : ""}o;
    if(size >= twoheap.length) {
      // Grow by one layer.
      twoheap = Arrays.copyOf(twoheap, twoheap.length + twoheap.length + 1);
    }
    final int twopos = size;
    twoheap[twopos] = co;
    ++size;
    heapifyUp(twopos, co);
  }

  @Override
  public void add(${type} key, int max) {
    if(size < max) {
      add(key);
    }
    else if(${getProperty("cmp")(">", "twoheap[0]", "key")}) {
      replaceTopElement(key);
    }
  }

  @Override
  ${raw != type ? "@SuppressWarnings(\"unchecked\")\n  " : ""}public ${type} replaceTopElement(${type} reinsert) {
    final ${raw} ret = twoheap[0];
    heapifyDown(${raw != type && raw != "Object" ? "(" + raw + ") " : ""}reinsert);
    return ${raw != type ? "(" + type + ") " : ""}ret;
  }

  /**
   * Heapify-Up method for 2-ary heap.
   *
   * @param twopos Position in 2-ary heap.
   * @param cur Current object
   */
  private void heapifyUp(int twopos, ${raw} cur) {
    while(twopos > 0) {
      final int parent = (twopos - 1) >>> 1;
      ${raw} par = twoheap[parent];
      if(${getProperty("cmp")("<=", "cur", "par")}) {
        break;
      }
      twoheap[twopos] = par;
      twopos = parent;
    }
    twoheap[twopos] = cur;
  }

  @Override
  ${raw != type ? "@SuppressWarnings(\"unchecked\")\n  " : ""}public ${type} poll() {
    final ${raw} ret = twoheap[0];
    --size;
    // Replacement object:
    if(size > 0) {
      final ${raw} reinsert = twoheap[size];
      twoheap[size] = ${zero};
      heapifyDown(reinsert);
    }
    else {
      twoheap[0] = ${zero};
    }
    return ${raw != type ? "(" + type + ") " : ""}ret;
  }

  /**
   * Invoke heapify-down for the root object.
   *
   * @param cur Object to insert.
   */
  private void heapifyDown(${raw} cur) {
    final int stop = size >>> 1;
    int twopos = 0;
    while(twopos < stop) {
      int bestchild = (twopos << 1) + 1;
      ${raw} best = twoheap[bestchild];
      final int right = bestchild + 1;
      if(right < size && ${getProperty("cmp")("<", "best", "twoheap[right]")}) {
        bestchild = right;
        best = twoheap[right];
      }
      if(${getProperty("cmp")("<=", "best", "cur")}) {
        break;
      }
      twoheap[twopos] = best;
      twopos = bestchild;
    }
    twoheap[twopos] = cur;
  }

  @Override
  ${raw != type ? "@SuppressWarnings(\"unchecked\")\n  " : ""}public ${type} peek() {
    return ${raw != type ? "(" + type + ") " : ""}twoheap[0];
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(${classname}.class.getSimpleName()).append(" [");
    for(UnsortedIter iter = new UnsortedIter(); iter.valid(); iter.advance()) {
      buf.append(iter.get()).append(',');
    }
    buf.append(']');
    return buf.toString();
  }

  @Override
  public UnsortedIter unsortedIter() {
    return new UnsortedIter();
  }

  /**
   * Unsorted iterator - in heap order. Does not poll the heap.
   *
   * Use this class as follows:
   *
   * <pre>
   * {@code
   * for (${boxed.equals("Comparable") ? "Object" : boxed}Heap.UnsortedIter${genu} iter = heap.unsortedIter(); iter.valid(); iter.next()) {
   *   doSomething(iter.get());
   * }
   * }
   * </pre>
   *
   * @author Erich Schubert
   */
  private class UnsortedIter implements ${boxed.equals("Comparable") ? "Object" : boxed}Heap.UnsortedIter${genu} {
    /**
     * Iterator position.
     */
    protected int pos = 0;

    @Override
    public boolean valid() {
      return pos < size;
    }

    @Override
    public UnsortedIter advance() {
      pos++;
      return this;
    }

    @Override
    ${raw != type ? "@SuppressWarnings(\"unchecked\")\n    " : ""}public ${type} get() {
      return ${raw != type ? "(" + type + ") " : ""}twoheap[pos];
    }
  }
}
