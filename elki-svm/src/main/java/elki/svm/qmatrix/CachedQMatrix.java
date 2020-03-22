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

import elki.utilities.datastructures.arrays.ArrayUtil;

//
// Kernel Cache
//
// l is the number of total data items
// size is the cache size limit in bytes
//
/**
 * This is the original cache from the libSVN implementation.
 * The code is very C stylish, and probably not half as effective on Java due to
 * garbage collection.
 */
public final class CachedQMatrix implements QMatrix {
  private static final long MEGABYTES = 1 << 20;

  private long size;

  private static final class head_t {
    head_t prev, next; // a circular list

    float[] data;

    int len; // data[0,len) is cached in this entry
  }

  private final head_t[] head;

  private head_t lru_head;

  QMatrix inner;

  public CachedQMatrix(int l, double cache_size, QMatrix inner) {
    this(l, (long) (cache_size * MEGABYTES), inner);
  }

  public CachedQMatrix(int l, long size_, QMatrix inner) {
    this.inner = inner;
    head = new head_t[l];
    for(int i = 0; i < l; i++) {
      head[i] = new head_t();
    }
    // 8 bytes chaining, 4 bytes len, 4 bytes data ref,
    // + 4 bytes in head array + 8 bytes Java object overhead
    size = size_ - l * 28;
    size >>= 2; // Bytes to floats.
    // Minimum cache size is two columns:
    size = Math.max(size, 2 * l);
    lru_head = new head_t();
    lru_head.next = lru_head.prev = lru_head;
  }

  @Override
  public void initialize() {
    inner.initialize();
  }

  private void lru_delete(head_t h) {
    // delete from current location
    h.prev.next = h.next;
    h.next.prev = h.prev;
  }

  private void lru_insert(head_t h) {
    // insert to last position
    h.next = lru_head;
    h.prev = lru_head.prev;
    h.prev.next = h;
    h.next.prev = h;
  }

  float[] get_data(int index, int len) {
    head_t h = head[index];
    if(h.len > 0) {
      lru_delete(h);
    }
    final int more = len - h.len;
    if(more > 0) {
      // free old space
      while(size < more) {
        head_t old = lru_head.next;
        lru_delete(old);
        size += old.len;
        old.data = null;
        old.len = 0;
      }

      // allocate new space
      float[] new_data = new float[len];
      if(h.data != null) {
        System.arraycopy(h.data, 0, new_data, 0, h.len);
      }
      // Compute missing distances:
      for(int j = h.len; j < len; ++j) {
        new_data[j] = (float) inner.similarity(index, j);
      }
      h.data = new_data;
      h.len = len;
      size -= more;
    }

    if(h.len > 0) {
      lru_insert(h);
    }
    return h.data;
  }

  @Override
  public void swap_index(int i, int j) {
    if(i == j) {
      return;
    }
    // Swap in index:
    ArrayUtil.swap(head, i, j);

    // Ensure i < j
    if(i > j) {
      int tmp = i;
      i = j;
      j = tmp;
    }
    // Swap in cached lists:
    for(head_t h = lru_head.next; h != lru_head; h = h.next) {
      if(h.len > i) {
        if(h.len > j) {
          ArrayUtil.swap(h.data, i, j);
        }
        else {
          // Discard this cache:
          lru_delete(h);
          size += h.len;
          h.data = null;
          h.len = 0;
        }
      }
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
