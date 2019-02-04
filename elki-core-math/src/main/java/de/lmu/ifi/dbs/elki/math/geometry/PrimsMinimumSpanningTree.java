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
package de.lmu.ifi.dbs.elki.math.geometry;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Prim's algorithm for finding the minimum spanning tree.
 * <p>
 * Implementation for <em>dense</em> graphs, represented as distance matrix.
 * <p>
 * Reference:
 * <p>
 * R. C. Prim<br>
 * Shortest connection networks and some generalizations<br>
 * Bell System Technical Journal 36
 * 
 * @author Erich Schubert
 * @since 0.5.5
 * 
 * @composed - - - Adapter
 * @composed - - - Collector
 */
@Reference(authors = "R. C. Prim", //
    title = "Shortest connection networks and some generalizations", //
    booktitle = "Bell System Technical Journal, 36 (1957)", //
    url = "https://doi.org/10.1002/j.1538-7305.1957.tb01515.x", //
    bibkey = "doi:10.1002/j.1538-7305.1957.tb01515.x")
public class PrimsMinimumSpanningTree {
  /**
   * Adapter class for double[][] matrixes.
   */
  public static final Array2DAdapter ARRAY2D_ADAPTER = new Array2DAdapter();

  /**
   * Process a k x k distance matrix.
   * 
   * @param mat Distance matrix
   * @return list of node number pairs representing the edges
   */
  public static int[] processDense(double[][] mat) {
    return processDense(mat, ARRAY2D_ADAPTER);
  }

  /**
   * Run Prim's algorithm on a dense graph.
   * 
   * @param data Data set
   * @param adapter Adapter instance
   * @return list of node number pairs representing the edges
   */
  public static <T> int[] processDense(T data, Adapter<T> adapter) {
    // Number of nodes
    final int n = adapter.size(data);
    // Output array storage
    int[] mst = new int[(n - 1) << 1];
    // Best distance for each node
    double[] best = new double[n];
    Arrays.fill(best, Double.POSITIVE_INFINITY);
    // Best previous node
    int[] src = new int[n];
    // Nodes already handled
    // byte[] uses more memory, but it will be faster.
    byte[] connected = new byte[n];

    // We always start at "random" node 0
    // Note: we use this below in the "j" loop!
    int current = 0;
    connected[current] = 1;
    best[current] = 0;

    // Search
    for(int i = n - 2; i >= 0; i--) {
      // Update best and src from current:
      int newbesti = -1;
      double newbestd = Double.POSITIVE_INFINITY;
      // Note: we assume we started with 0, and can thus skip it
      for(int j = 0; j < n; ++j) {
        if(connected[j] == 1) {
          continue;
        }
        final double dist = adapter.distance(data, current, j);
        if(dist < best[j]) {
          best[j] = dist;
          src[j] = current;
        }
        if(best[j] < newbestd || newbesti == -1) {
          newbestd = best[j];
          newbesti = j;
        }
      }
      assert (newbesti >= 0);
      // Flag
      connected[newbesti] = 1;
      // Store edge
      mst[i << 1] = newbesti;
      mst[(i << 1) + 1] = src[newbesti];
      // Continue
      current = newbesti;
    }
    return mst;
  }

  /**
   * Run Prim's algorithm on a dense graph.
   * 
   * @param data Data set
   * @param adapter Adapter instance
   * @param collector Edge collector
   */
  public static <T> void processDense(T data, Adapter<T> adapter, Collector collector) {
    // Number of nodes
    final int n = adapter.size(data);
    // Best distance for each node
    double[] best = new double[n];
    Arrays.fill(best, Double.POSITIVE_INFINITY);
    // Best previous node
    int[] src = new int[n];
    // Nodes already handled
    // byte[] uses more memory, but it will be faster.
    byte[] connected = new byte[n];

    // We always start at "random" node 0
    // Note: we use this below in the "j" loop!
    int current = 0;
    connected[current] = 1;
    best[current] = 0;

    // Search
    for(int i = n - 2; i >= 0; i--) {
      // Update best and src from current:
      int newbesti = -1;
      double newbestd = Double.POSITIVE_INFINITY;
      // Note: we assume we started with 0, and can thus skip it
      for(int j = 0; j < n; ++j) {
        if(connected[j] == 1) {
          continue;
        }
        final double dist = adapter.distance(data, current, j);
        if(dist < best[j]) {
          best[j] = dist;
          src[j] = current;
        }
        if(best[j] < newbestd || newbesti == -1) {
          newbestd = best[j];
          newbesti = j;
        }
      }
      assert (newbesti >= 0);
      // Flag
      connected[newbesti] = 1;
      // Store edge
      collector.addEdge(newbestd, src[newbesti], newbesti);
      // Continue
      current = newbesti;
    }
  }

  /**
   * Prune the minimum spanning tree, removing all edges to nodes that have a
   * degree below {@code minDegree}.
   * 
   * @param numnodes Number of nodes (MUST use numbers 0 to {@code numnodes-1})
   * @param tree Original spanning tree
   * @param minDegree Minimum node degree
   * @return Pruned spanning tree
   */
  public static int[] pruneTree(int numnodes, int[] tree, int minDegree) {
    // Compute node degrees
    int[] deg = new int[numnodes];
    for(int i = 0; i < tree.length; i++) {
      deg[tree[i]]++;
    }
    // Count nodes to be retained:
    int keep = 0;
    for(int i = 1; i < tree.length; i += 2) {
      if(deg[tree[i - 1]] >= minDegree && deg[tree[i]] >= minDegree) {
        keep++;
      }
    }
    // Build reduced tree
    int j = 0;
    int[] ret = new int[keep];
    for(int i = 1; i < tree.length; i += 2) {
      if(deg[tree[i - 1]] >= minDegree && deg[tree[i]] >= minDegree) {
        ret[j++] = tree[i - 1];
        ret[j++] = tree[i];
      }
    }
    assert (j == ret.length);
    return ret;
  }

  /**
   * Adapter interface to allow use with different data representations.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Data reference
   */
  public interface Adapter<T> {
    /**
     * Get the distance of two objects
     * 
     * @param data Data set
     * @param i First index
     * @param j Second index
     * @return Distance of objects number i and number j.
     */
    double distance(T data, int i, int j);

    /**
     * Get number of objects in dataset
     * 
     * @return Size
     */
    int size(T data);
  }

  /**
   * Interface for collecting edges.
   * 
   * @author Erich Schubert
   */
  @FunctionalInterface
  public interface Collector {
    /**
     * Add a new edge to the output.
     * 
     * @param length Length of edge
     * @param i Source node
     * @param j Destination node
     */
    void addEdge(double length, int i, int j);
  }

  /**
   * Adapter for a simple 2d double matrix.
   * 
   * @author Erich Schubert
   */
  public static class Array2DAdapter implements Adapter<double[][]> {
    /**
     * Constructor. Use static instance!
     */
    private Array2DAdapter() {
      // Use static instance!
    }

    @Override
    public double distance(double[][] data, int i, int j) {
      return data[i][j];
    }

    @Override
    public int size(double[][] data) {
      return data.length;
    }
  }
}
