package experimentalcode.shared.parallelcoord;

import java.util.Arrays;
import java.util.BitSet;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
 */
@Reference(authors = "R. C. Prim", title = "Shortest connection networks and some generalizations", booktitle = "Bell System Technical Journal, 36 (1957)")
public class PrimsMinimumSpanningTree {
  /**
   * Process a k x k distance matrix.
   * 
   * @param mat Distance matrix
   * @return list of node number pairs representing the edges
   */
  public static int[] run(double[][] mat) {
    // Number of nodes
    final int n = mat.length;
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
      // Note: we assume we started with 0.
      for (int j = 1; j < n; j++) {
        if (!in.get(j)) {
          if (mat[current][j] < best[j]) {
            best[j] = mat[current][j];
            src[j] = current;
          }
          if (best[j] < newbestd) {
            newbestd = best[j];
            newbesti = j;
          }
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

  // FIXME: turn into a proper unit test.
  public static void main(String[] args) {
    // A simple test.
    final double inf = Double.POSITIVE_INFINITY;
    double[][] mat = new double[][] {//
    { 0.0, 7.0, inf, 5.0, inf, inf, inf }, //
    { 7.0, 0.0, 8.0, 9.0, 7.0, inf, inf }, //
    { inf, 8.0, 0.0, inf, 5.0, inf, inf }, //
    { 5.0, 9.0, inf, 0.0, 15., 6.0, inf }, //
    { inf, 7.0, 5.0, 15., 0.0, 8.0, 9.0 }, //
    { inf, inf, inf, 6.0, 8.0, 0.0, 11. }, //
    { inf, inf, inf, inf, 9.0, 11., 0.0 }, //
    };
    int[] ret = run(mat);
    // "correct" edges (ignore order and direction!)
    int[] correct = new int[] { 0, 1, 0, 4, 1, 5, 2, 5, 5, 7, 4, 6 };
    for (int i = 0; i < ret.length; i += 2) {
      System.out.println((char) ('A' + ret[i]) + " -> " + (char) ('A' + ret[i + 1]));
    }
  }
}
