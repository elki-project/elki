/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.utilities.datastructures.unionfind;

import java.util.Arrays;

import elki.utilities.documentation.Reference;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Union-find algorithm for primitive integers, with optimizations.
 * <p>
 * This is the weighted quick union approach, weighted by count and using
 * path-halving for optimization.
 * <p>
 * Reference:
 * <p>
 * R. Sedgewick<br>
 * 1.3 Union-Find Algorithms<br>
 * Algorithms in C, Parts 1-4
 *
 * @author Erich Schubert
 * @since 0.7.1
 */
@Reference(authors = "R. Sedgewick", //
    title = "Algorithms in C, Parts 1-4", //
    booktitle = "", //
    bibkey = "DBLP:books/daglib/0004943")
public class WeightedQuickUnionInteger {
  /**
   * Number of used indexes.
   */
  private int used;

  /**
   * Parent element
   */
  private int[] parent;

  /**
   * Weight, for optimization.
   */
  private int[] weight;

  /**
   * Initial size.
   */
  private static final int INITIAL_SIZE = 51;

  /**
   * Constructor.
   */
  public WeightedQuickUnionInteger() {
    this(INITIAL_SIZE);
  }

  /**
   * Constructor.
   * 
   * @param size Initial size
   */
  public WeightedQuickUnionInteger(int size) {
    weight = new int[size];
    parent = new int[size];
  }

  /**
   * Initialize with a 1:1 mapping and weight 1.
   * 
   * @param size Size
   * @return {@code this}, for call chaining.
   */
  public WeightedQuickUnionInteger fullInit(int size) {
    assert used == 0;
    if(size >= parent.length) {
      this.parent = new int[size];
      this.weight = new int[size];
    }
    for(int i = 0; i < size; i++) {
      parent[i] = i;
      weight[i] = 1;
    }
    used = size;
    return this;
  }

  /**
   * Occupy the next unused index.
   *
   * @param weight Initial weight.
   * @return Next unused index.
   */
  public int nextIndex(int weight) {
    if(used == parent.length) {
      int nsize = used + (used >> 1);
      this.weight = Arrays.copyOf(this.weight, nsize);
      this.parent = Arrays.copyOf(this.parent, nsize);
    }
    this.weight[used] = weight;
    this.parent[used] = used;
    return used++;
  }

  /**
   * Find the parent of an object.
   *
   * @param cur Current entry
   * @return Parent entry
   */
  public int find(int cur) {
    assert (cur >= 0 && cur < parent.length);
    int p = parent[cur], tmp;
    while(cur != p) {
      tmp = p;
      p = parent[cur] = parent[p]; // Perform simple path compression.
      cur = tmp;
    }
    return cur;
  }

  /**
   * Join the components of elements p and q.
   *
   * @param first First element
   * @param second Second element
   * @return Component id.
   */
  public int union(int first, int second) {
    int firstComponent = find(first), secondComponent = find(second);
    if(firstComponent == secondComponent) {
      return firstComponent;
    }
    final int w1 = weight[firstComponent], w2 = weight[secondComponent];
    if(w1 > w2) {
      parent[secondComponent] = firstComponent;
      weight[firstComponent] += w2;
      return firstComponent;
    }
    else {
      parent[firstComponent] = secondComponent;
      weight[secondComponent] += w1;
      return secondComponent;
    }
  }

  /**
   * Test if two components are connected.
   *
   * @param first First element
   * @param second Second element
   * @return {@code true} if they are in the same component.
   */
  public boolean isConnected(int first, int second) {
    return find(first) == find(second);
  }

  /**
   * Collect all component root elements.
   *
   * @return Root elements
   */
  public IntList getRoots() {
    IntList roots = new IntArrayList();
    for(int i = 0; i < used; i++) {
      // roots or one element in component
      if(parent[i] == i) {
        roots.add(i);
      }
    }
    return roots;
  }

  /**
   * Get the weight of a component.
   * 
   * @param component id
   * @return Weight
   */
  public int size(int component) {
    return weight[component];
  }

  /**
   * Number of allocated indexes.
   *
   * @return Index number.
   */
  public int size() {
    return used;
  }
}
