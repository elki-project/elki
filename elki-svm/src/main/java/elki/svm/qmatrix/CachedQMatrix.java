/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * This code is directly derived from libSVM, and hence the libSVM copyright
 * apply:
 * 
 * Copyright (c) 2000-2019 Chih-Chung Chang and Chih-Jen Lin
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * 3. Neither name of copyright holders nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package elki.svm.qmatrix;

import java.util.Arrays;

import elki.utilities.datastructures.arrays.ArrayUtil;

/**
 * This is the original cache from the libSVN implementation.
 * The code is very C stylish, and probably not half as effective on Java due to
 * garbage collection.
 */
public final class CachedQMatrix implements QMatrix {
  /**
   * Constant for parameterization in megabytes
   */
  private static final long MEGABYTES = 1 << 20;

  /**
   * Remaining memory (in floats)
   */
  private long size;

  /**
   * Wrapped inner matrix
   */
  private final QMatrix inner;

  /**
   * Data storage
   */
  private final float[][] data;

  /**
   * Number of valid entries in each buffer
   */
  private final int[] len;

  /**
   * LRU head
   */
  private int lru = 0;

  /**
   * LRU: backward and forward chaining
   */
  private final int[] chain;

  public CachedQMatrix(int l, double cache_size, QMatrix inner) {
    this(l, (long) (cache_size * MEGABYTES), inner);
  }

  public CachedQMatrix(int l, long size_, QMatrix inner) {
    this.inner = inner;
    chain = new int[l << 1]; // zeros
    for(int i = 0; i < l; i++) {
      selflink(i);
    }
    len = new int[l];
    data = new float[l][];
    // Per entry: 8 bytes chain, 4 bytes len
    // data: each used entry will cost 24, too.
    // Each of these arrays: 24 bytes overhead (once)
    // This structure: ~32 bytes (note: for compressed pointers!)
    size = size_ - l * 36L - 24 * 4 - 32;
    size >>= 2; // Bytes to floats.
    // Minimum feasible cache size is two columns:
    size = Math.max(size, l << 1);
  }

  @Override
  public void initialize() {
    inner.initialize();
  }

  /**
   * Get the next element in the LRU chain.
   *
   * @param i Entry
   * @return Next
   */
  private final int next(int i) {
    return chain[i << 1];
  }

  /**
   * Get the previous element in the LRU chain.
   *
   * @param i Entry
   * @return Prev
   */
  private final int prev(int i) {
    return chain[(i << 1) + 1];
  }

  /**
   * Link two objects in the LRU chain.
   *
   * @param p Previous object
   * @param n Next pbject
   */
  private final void link(int p, int n) {
    chain[p << 1] = n;
    chain[(n << 1) + 1] = p;
  }

  /**
   * Link an element to itself only.
   * 
   * @param p Element to link
   */
  private final void selflink(int p) {
    int p2 = p << 1;
    chain[p2++] = chain[p2] = p;
  }

  /**
   * Insert into the last position of the LRU list.
   *
   * @param h Object to be inserted
   */
  private final void lru_insert(int h) {
    assert next(h) == h && prev(h) == h : h + " already linked";
    link(prev(lru), h);
    link(h, lru);
  }

  /**
   * Delete from the LRU list.
   *
   * @param h Object index to be deleted
   */
  private final void lru_delete(int h) {
    final int next = next(h);
    lru = lru == h ? next : lru;
    link(prev(h), next);
    selflink(h);
  }

  /**
   * Discard a cache entry.
   *
   * @param h entry index
   */
  private final void discard(int h) {
    lru_delete(h);
    size += data[h].length;
    data[h] = null;
    len[h] = 0;
  }

  float[] get_data(int h, int len) {
    int hlen = this.len[h];
    float[] hdata = data[h];
    if(hlen > 0) {
      lru_delete(h);
    }
    final int more = len - hlen;
    if(more > 0) {
      while(size < more) {
        discard(lru != h ? lru : h);
      }

      size -= len - (hdata != null ? hdata.length : 0);
      float[] new_data = hdata != null ? Arrays.copyOf(hdata, len) : new float[len];
      for(int j = hlen; j < len; ++j) {
        new_data[j] = (float) inner.similarity(h, j);
      }
      hdata = data[h] = new_data;
      hlen = this.len[h] = len;
    }
    if(hlen > 0) {
      lru_insert(h);
    }
    return hdata;
  }

  @Override
  public void swap_index(int i, int j) {
    if(i == j) {
      return;
    }
    // Ensure i < j
    if(i > j) {
      int tmp = i;
      i = j;
      j = tmp;
    }
    // Swap in index:
    ArrayUtil.swap(data, i, j);
    ArrayUtil.swap(len, i, j);
    // Update chain:
    int nei = next(i), pri = prev(i), nej = next(j), prj = prev(j);
    // System.err.println("Chain: " + prev(i) + " > " + next(prev(i)) + "=" + i
    // + "=" + prev(next(i)) + " > " + next(i) + " " + prev(j) + " > " +
    // next(prev(j)) + "=" + j + "=" + prev(next(j)) + " > " + next(j));
    assert prev(next(i)) == i && next(prev(i)) == i : prev(next(i)) + " != " + next(prev(i)) + " != " + i;
    assert prev(next(j)) == j && next(prev(j)) == j : prev(next(j)) + " != " + next(prev(j)) + " != " + j;
    assert (pri == i) == (nei == i) : "Double-linked list inconsistent.";
    assert (prj == j) == (nej == j) : "Double-linked list inconsistent.";
    if(pri != j || nei != j) { // Avoid mutual-link special case on low memory.
      if(prj == j) {
        selflink(i);
      }
      else {
        link(prj != i ? prj : j, i);
        link(i, nej != i ? nej : j);
      }
      if(pri == i) {
        selflink(j);
      }
      else {
        link(pri != j ? pri : i, j);
        link(j, nei != j ? nei : i);
      }
    }
    // System.err.println("After: " + prev(i) + " > " + next(prev(i)) + "=" + i
    // + "=" + prev(next(i)) + " > " + next(i) + " " + prev(j) + " > " +
    // next(prev(j)) + "=" + j + "=" + prev(next(j)) + " > " + next(j));
    assert prev(next(i)) == i && next(prev(i)) == i : prev(next(i)) + " != " + next(prev(i)) + " != " + i;
    assert prev(next(j)) == j && next(prev(j)) == j : prev(next(j)) + " != " + next(prev(j)) + " != " + j;
    lru = i == lru ? j : j == lru ? i : lru;

    // Swap in cached lists:
    for(int h = lru, next = -1; next != lru; h = next) {
      int prev = prev(h);
      next = next(h); // could get trashed by discard below
      final int lenh = len[h];
      if(lenh > j) {
        ArrayUtil.swap(data[h], i, j);
      }
      else if(lenh > i /* but < j */) {
        discard(h); // Don't have the value for j.
        // TODO: rather fill the one missing value instead?
        // TODO: or just reduce lenh?
        assert prev(next) == prev;
        assert next == next(prev);
      } // else: contains neither i nor j.
    }

    inner.swap_index(i, j);
  }

  @Override
  public void get_Q(int column, int len, float[] out) {
    float[] data = get_data(column, len);
    if(out != null) { // Pre-cache only
      System.arraycopy(data, 0, out, 0, len);
    }
  }

  @Override
  public double[] get_QD() {
    // TODO: cache QD here instead?
    return inner.get_QD(); // Pass-through
  }

  @Override
  public double similarity(int i, int j) {
    return inner.similarity(i, j);
  }
}
