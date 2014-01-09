package de.lmu.ifi.dbs.elki.math.geometry;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import java.util.Arrays;
import java.util.BitSet;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Prim's algorithm for finding the minimum spanning tree.
 * 
 * Implementation for <em>dense</em> graphs, represented as distance matrix.
 * 
 * Reference:
 * <p>
 * R. C. Prim<br />
 * Shortest connection networks and some generalizations<br />
 * In: Bell System Technical Journal, 36 (1957), pp. 1389–140
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf Adapter
 */
@Reference(authors = "R. C. Prim", title = "Shortest connection networks and some generalizations", booktitle = "Bell System Technical Journal, 36 (1957)")
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
   * Process a k x k distance matrix.
   * 
   * @param mat Distance matrix
   * @return list of node number pairs representing the edges
   */
  public static int[] processDense(Matrix mat) {
    return processDense(mat.getArrayRef(), ARRAY2D_ADAPTER);
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
    BitSet in = new BitSet(n);

    // We always start at "random" node 0
    // Note: we use this below in the "j" loop!
    int current = 0;
    in.set(current);
    best[current] = 0;

    // Search
    for (int i = n - 2; i >= 0; i--) {
      // Update best and src from current:
      int newbesti = -1;
      double newbestd = Double.POSITIVE_INFINITY;
      // Note: we assume we started with 0, and can thus skip it
      for (int j = in.nextClearBit(1); j < n && j > 0; j = in.nextClearBit(j + 1)) {
        final double dist = adapter.distance(data, current, j);
        if (dist < best[j]) {
          best[j] = dist;
          src[j] = current;
        }
        if (best[j] < newbestd) {
          newbestd = best[j];
          newbesti = j;
        }
      }
      assert (newbesti >= 0);
      // Flag
      in.set(newbesti);
      // Store edge
      mst[i << 1] = newbesti;
      mst[(i << 1) + 1] = src[newbesti];
      // Continue
      current = newbesti;
    }
    return mst;
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
    public double distance(T data, int i, int j);

    /**
     * Get number of objects in dataset
     * 
     * @return Size
     */
    public int size(T data);
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
