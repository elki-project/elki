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
 * Binary heap for ${key.type != "K" ? key.type : "Object"} keys and ${val.type != "V" ? val.type : "Object"} values.
 *
 * This class is generated from a template.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @has - - - UnsortedIter
${key.genu || val.genu ? " *\n" : ""}${key.genu ? " * @param "+key.genu+" Key type\n" : ""}${val.genu ? " * @param "+val.genu+" Value type\n" : ""} */
public class ${classname}${key.gend}${val.gend} implements ${key.boxed.equals("Comparable") ? "Object" : key.boxed}${val.boxed.equals("Comparable") ? "Object" : val.boxed}Heap${key.genu}${val.genu} {
  /**
   * Base heap.
   */
  protected ${key.raw}[] twoheap;

  /**
   * Base heap values.
   */
  protected ${val.raw}[] twovals;

  /**
   * Current size of heap.
   */
  protected int size;

  /**
   * Initial size of the 2-ary heap.
   */
  private final static int TWO_HEAP_INITIAL_SIZE = (1 << 5) - 1;

  /**
   * Constructor, with default size.
   */
  public ${classname}() {
    super();
    ${key.raw}[] twoheap = new ${key.raw}[TWO_HEAP_INITIAL_SIZE];
    ${val.raw}[] twovals = new ${val.raw}[TWO_HEAP_INITIAL_SIZE];

    this.twoheap = twoheap;
    this.twovals = twovals;
  }

  /**
   * Constructor, with given minimum size.
   *
   * @param minsize Minimum size
   */
  public ${classname}(int minsize) {
    super();
    final int size = HeapUtil.nextPow2Int(minsize + 1) - 1;
    ${key.raw}[] twoheap = new ${key.raw}[size];
    ${val.raw}[] twovals = new ${val.raw}[size];

    this.twoheap = twoheap;
    this.twovals = twovals;
  }

  @Override
  public void clear() {
    size = 0;
    Arrays.fill(twoheap, ${key.zero});
    Arrays.fill(twovals, ${val.zero});
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
  ${key.raw != key.type ? "@SuppressWarnings(\"unchecked\")\n  " : ""}public void add(${key.type} o, ${val.type} v) {
    final ${key.raw} co = ${key.raw != key.type ? "(" + key.raw + ") " : ""}o;
    final ${val.raw} cv = ${val.raw != val.type ? "(" + val.raw + ") " : ""}v;
    if(size >= twoheap.length) {
      // Grow by one layer.
      twoheap = Arrays.copyOf(twoheap, twoheap.length + twoheap.length + 1);
      twovals = Arrays.copyOf(twovals, twovals.length + twovals.length + 1);
    }
    final int twopos = size;
    twoheap[twopos] = co;
    twovals[twopos] = cv;
    ++size;
    heapifyUp(twopos, co, cv);
  }

  @Override
  public void add(${key.type} key, ${val.type} val, int max) {
    if(size < max) {
      add(key, val);
    }
    else if(${key.boxed.startsWith("Comparable") ? "twoheap[0].compareTo(key) > 0" : "twoheap[0] > key"}) {
      replaceTopElement(key, val);
    }
  }

  @Override
  ${key.raw != key.type ? "@SuppressWarnings(\"unchecked\")\n  " : ""}public void replaceTopElement(${key.type} reinsert, ${val.type} val) {
    heapifyDown(${key.raw != key.type ? "(" + key.raw + ") " : ""}reinsert, ${val.raw != val.type ? "(" + val.raw + ") " : ""}val);
  }

  /**
   * Heapify-Up method for 2-ary heap.
   *
   * @param twopos Position in 2-ary heap.
   * @param cur Current object
   * @param val Current value
   */
  private void heapifyUp(int twopos, ${key.raw} cur, ${val.raw} val) {
    while(twopos > 0) {
      final int parent = (twopos - 1) >>> 1;
      ${key.raw} par = twoheap[parent];
      if(${key.boxed.startsWith("Comparable") ? "cur.compareTo(par) <= 0" : "cur <= par"}) {
        break;
      }
      twoheap[twopos] = par;
      twovals[twopos] = twovals[parent];
      twopos = parent;
    }
    twoheap[twopos] = cur;
    twovals[twopos] = val;
  }

  @Override
  ${key.raw != key.type ? "@SuppressWarnings(\"unchecked\")\n  " : ""}public void poll() {
    --size;
    // Replacement object:
    if(size > 0) {
      final ${key.raw} reinsert = twoheap[size];
      final ${val.raw} reinsertv = twovals[size];
      twoheap[size] = ${key.zero};
      twovals[size] = ${val.zero};
      heapifyDown(reinsert, reinsertv);
    }
    else {
      twoheap[0] = ${key.zero};
      twovals[0] = ${val.zero};
    }
  }

  /**
   * Invoke heapify-down for the root object.
   *
   * @param cur Object to insert.
   * @param val Value to reinsert.
   */
  private void heapifyDown(${key.raw} cur, ${val.raw} val) {
    final int stop = size >>> 1;
    int twopos = 0;
    while(twopos < stop) {
      int bestchild = (twopos << 1) + 1;
      ${key.raw} best = twoheap[bestchild];
      final int right = bestchild + 1;
      if(right < size && ${key.boxed.startsWith("Comparable") ? "best.compareTo(twoheap[right]) <= 0" : "best <= twoheap[right]"}) {
        bestchild = right;
        best = twoheap[right];
      }
      if(${key.boxed.startsWith("Comparable") ? "best.compareTo(cur) <= 0" : "best <= cur"}) {
        break;
      }
      twoheap[twopos] = best;
      twovals[twopos] = twovals[bestchild];
      twopos = bestchild;
    }
    twoheap[twopos] = cur;
    twovals[twopos] = val;
  }

  @Override
  ${key.raw != key.type ? "@SuppressWarnings(\"unchecked\")\n  " : ""}public ${key.type} peekKey() {
    return ${key.raw != key.type ? "(" + key.type + ") " : ""}twoheap[0];
  }

  @Override
  ${val.raw != val.type ? "@SuppressWarnings(\"unchecked\")\n  " : ""}public ${val.type} peekValue() {
    return ${val.raw != val.type ? "(" + val.type + ") " : ""}twovals[0];
  }

  @Override
  public boolean containsKey(${key.type} q) {
    for(int pos = 0; pos < size; pos++) {
      if(${key.boxed.startsWith("Comparable") || key.boxed.startsWith("Object") ? "q.equals(twoheap[pos])" : "twoheap[pos] == q"}) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsValue(${val.type} q) {
    for(int pos = 0; pos < size; pos++) {
      if(${val.boxed.startsWith("Comparable") || val.boxed.startsWith("Object") ? "q.equals(twovals[pos])" : "twovals[pos] == q"}) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(${classname}.class.getSimpleName()).append(" [");
    for(UnsortedIter iter = new UnsortedIter(); iter.valid(); iter.advance()) {
      buf.append(iter.getKey()).append(':').append(iter.getValue()).append(',');
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
   * for (${key.boxed.equals("Comparable") ? "Object" : key.boxed}${val.boxed.equals("Comparable") ? "Object" : val.boxed}Heap.UnsortedIter${key.genu}${val.genu} iter = heap.unsortedIter(); iter.valid(); iter.next()) {
   *   doSomething(iter.get());
   * }
   * }
   * </pre>
   *
   * @author Erich Schubert
   */
  private class UnsortedIter implements ${key.boxed.equals("Comparable") ? "Object" : key.boxed}${val.boxed.equals("Comparable") ? "Object" : val.boxed}Heap.UnsortedIter${key.genu}${val.genu} {
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
    ${key.raw != key.type ? "@SuppressWarnings(\"unchecked\")\n    " : ""}public ${key.type} getKey() {
      return ${key.raw != key.type ? "(" + key.type + ") " : ""}twoheap[pos];
    }

    @Override
    ${val.raw != val.type ? "@SuppressWarnings(\"unchecked\")\n    " : ""}public ${val.type} getValue() {
      return ${val.raw != val.type ? "(" + val.type + ") " : ""}twovals[pos];
    }
  }
}
