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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.distribution.Assignments;
import de.lmu.ifi.dbs.elki.math.geometry.PrimsMinimumSpanningTree;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Splitting algorithm using the minimum spanning tree (MST), as proposed by the
 * Slim-Tree variant.
 * <p>
 * Unfortunately, the slim-tree paper does not detail how to choose the "most
 * appropriate edge from the longest ones" (to find a more balanced split), so
 * we try to longest 50%, and keep the choice which yields the most balanced
 * split. This seems to work quite well.
 * <p>
 * Reference:
 * <p>
 * C. Traina Jr., A. J. M. Traina, B. Seeger, C. Faloutsos<br>
 * Slim-Trees: High Performance Metric Trees Minimizing Overlap Between
 * Nodes<br>
 * Int. Conf. Extending Database Technology (EDBT'2000)
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <E> Entry type
 * @param <N> Node type
 */
@Priority(Priority.RECOMMENDED)
@Reference(authors = "C. Traina Jr., A. J. M. Traina, B. Seeger, C. Faloutsos", //
    title = "Slim-Trees: High Performance Metric Trees Minimizing Overlap Between Nodes", //
    booktitle = "Int. Conf. Extending Database Technology (EDBT'2000)", //
    url = "https://doi.org/10.1007/3-540-46439-5_4", //
    bibkey = "DBLP:conf/edbt/TrainaTSF00")
public class MSTSplit<E extends MTreeEntry, N extends AbstractMTreeNode<?, N, E>> implements MTreeSplit<E, N> {
  @Override
  public Assignments<E> split(AbstractMTree<?, N, E, ?> tree, N node) {
    double[][] matrix = AbstractMTreeSplit.computeDistanceMatrix(tree, node);
    int[] idx = mstPartition(matrix);
    assert (idx[0] == 0) : "First partition should have been merged into object 0.";
    // Find the representative for each partition, O(n^2):
    int r1 = 0, r2 = 1;
    double m1 = Double.POSITIVE_INFINITY, m2 = Double.POSITIVE_INFINITY;
    final int n = node.getNumEntries();
    for(int i = 0; i < n; i++) {
      double m = coverRadius(matrix, idx, i);
      if(idx[i] == 0) {
        if(m < m1) { // Min of first cluster
          m1 = m;
          r1 = i;
        }
      }
      else if(m < m2) { // Min of second cluster
        m2 = m;
        r2 = i;
      }
    }

    Assignments<E> assign = new Assignments<>(node.getEntry(r1).getRoutingObjectID(), node.getEntry(r2).getRoutingObjectID(), n - 1);
    double[] row1 = matrix[r1], row2 = matrix[r2];
    for(int i = 0; i < n; i++) {
      E ent = node.getEntry(i);
      if(idx[i] == 0) {
        assign.addToFirst(ent, row1[i]);
      }
      else {
        assert idx[i] == idx[r2] : "More than two partitions?";
        assign.addToSecond(ent, row2[i]);
      }
    }
    return assign;
  }

  /**
   * Find the cover radius of a partition.
   *
   * @param matrix Distance matrix
   * @param idx Partition keys
   * @param i Candidate index
   * @return max(d(i,j)) for all j with the same index
   */
  private static double coverRadius(double[][] matrix, int[] idx, int i) {
    final int idx_i = idx[i];
    final double[] row_i = matrix[i];
    double m = 0;
    for(int j = 0; j < row_i.length; j++) {
      if(i != j && idx_i == idx[j]) {
        final double d = row_i[j];
        m = d > m ? d : m;
      }
    }
    return m;
  }

  /**
   * Partition the data using the minimu spanning tree.
   *
   * @param matrix
   * @return partition assignments
   */
  private static int[] mstPartition(double[][] matrix) {
    final int n = matrix.length;
    int[] edges = PrimsMinimumSpanningTree.processDense(matrix);
    // Note: Prims does *not* yield edges sorted by edge length!
    double meanlength = thresholdLength(matrix, edges);
    int[] idx = new int[n], best = new int[n], sizes = new int[n];
    int bestsize = -1;
    double bestlen = 0;
    for(int omit = n - 2; omit > 0; --omit) {
      final double len = edgelength(matrix, edges, omit);
      if(len < meanlength) {
        continue;
      }
      omitEdge(edges, idx, sizes, omit);
      // Finalize array:
      int minsize = n;
      for(int i = 0; i < n; i++) {
        int j = idx[i] = follow(i, idx);
        if(j == i && sizes[i] < minsize) {
          minsize = sizes[i];
        }
      }
      if(minsize > bestsize || (minsize == bestsize && len > bestlen)) {
        bestsize = minsize;
        bestlen = len;
        System.arraycopy(idx, 0, best, 0, n);
      }
    }
    return best;
  }

  /**
   * Choose the threshold length of edges to consider omittig.
   *
   * @param matrix Distance matrix
   * @param edges Edges
   * @return Distance threshold
   */
  private static double thresholdLength(double[][] matrix, int[] edges) {
    double[] lengths = new double[edges.length >> 1];
    for(int i = 0, e = edges.length - 1; i < e; i += 2) {
      lengths[i >> 1] = matrix[edges[i]][edges[i + 1]];
    }
    Arrays.sort(lengths);
    final int pos = (lengths.length >> 1); // 50%
    return lengths[pos];
  }

  /**
   * Length of edge i.
   *
   * @param matrix Distance matrix
   * @param edges Edge list
   * @param i Edge number
   * @return Edge length
   */
  private static double edgelength(double[][] matrix, int[] edges, int i) {
    i <<= 1;
    return matrix[edges[i]][edges[i + 1]];
  }

  /**
   * Partition the data by omitting one edge.
   * 
   * @param edges Edges list
   * @param idx Partition index storage
   * @param sizes Partition sizes
   * @param omit Edge number to omit
   */
  private static void omitEdge(int[] edges, int[] idx, int[] sizes, int omit) {
    for(int i = 0; i < idx.length; i++) {
      idx[i] = i;
    }
    Arrays.fill(sizes, 1);
    for(int i = 0, j = 0, e = edges.length - 1; j < e; i++, j += 2) {
      if(i == omit) {
        continue;
      }
      int ea = edges[j + 1], eb = edges[j];
      if(eb < ea) { // Swap
        int tmp = eb;
        eb = ea;
        ea = tmp;
      }
      final int pa = follow(ea, idx), pb = follow(eb, idx);
      assert (pa != pb) : "Must be disjoint - MST inconsistent.";
      sizes[idx[pa]] += sizes[idx[pb]];
      idx[pb] = idx[pa];
    }
  }

  /**
   * Union-find with simple path compression.
   *
   * @param i Start
   * @param partitions Partitions array
   * @return Partition
   */
  private static int follow(int i, int[] partitions) {
    int next = partitions[i], tmp;
    while(i != next) {
      tmp = next;
      next = partitions[i] = partitions[next];
      i = tmp;
    }
    return i;
  }
}
