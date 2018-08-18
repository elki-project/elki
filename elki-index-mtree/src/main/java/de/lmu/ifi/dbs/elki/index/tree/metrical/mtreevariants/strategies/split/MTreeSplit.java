/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
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

import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.distribution.Assignments;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.distribution.BalancedDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;

/**
 * Abstract super class for splitting a node in an M-Tree.
 *
 * @author Elke Achtert
 * @since 0.2
 *
 * @apiviz.composedOf Assignments
 *
 * @param <E> the type of MTreeEntry used in the M-Tree
 * @param <N> the type of AbstractMTreeNode used in the M-Tree
 */
public abstract class MTreeSplit<E extends MTreeEntry, N extends AbstractMTreeNode<?, N, E>> {
  /**
   * Entry distribution strategy.
   */
  protected BalancedDistribution distributor = new BalancedDistribution();

  /**
   * Compute the pairwise distances in the given node.
   * 
   * @param tree Tree
   * @param node Node
   * @return Distance matrix
   */
  protected static <E extends MTreeEntry, N extends AbstractMTreeNode<?, N, E>> double[] computeDistanceMatrix(AbstractMTree<?, N, E, ?> tree, N node) {
    final int n = node.getNumEntries();
    double[] distancematrix = new double[n * n];
    // Build distance matrix
    for(int i = 0; i < n; i++) {
      E ei = node.getEntry(i);
      for(int j = i + 1, b = i * n + j; j < n; j++, b++) {
        double distance = tree.distance(ei, node.getEntry(j));
        distancematrix[b] = distance;
        distancematrix[j * n + i] = distance; // Symmetry
      }
    }
    return distancematrix;
  }

  /**
   * Extract the distances from a distance matrix.
   * 
   * @param distanceMatrix Distance matrix
   * @param n Number of entries
   * @param routing1 First routing object index
   * @param idx1 Output: neighbor index
   * @param dis1 Output: neighbor distance
   * @param routing2 Second routing object index
   * @param idx2 Output: neighbor index
   * @param dis2 Output: neighbor distance
   */
  protected static void distancesFromDistanceMatrix(double[] distanceMatrix, int n, int routing1, int[] idx1, double[] dis1, int routing2, int[] idx2, double[] dis2) {
    for(int i = 0, j = 0, off = 0; i < n; i++, off += n) {
      if(i == routing1 || i == routing2) {
        continue;
      }
      // Look up the distances of o to o1 / o2
      dis1[j] = distanceMatrix[off + routing1];
      idx1[j] = i;
      dis2[j] = distanceMatrix[off + routing2];
      idx2[j] = i;
      j++;
    }
    DoubleIntegerArrayQuickSort.sort(dis1, idx1, n - 2);
    DoubleIntegerArrayQuickSort.sort(dis2, idx2, n - 2);
  }

  /**
   * Returns the assignments of this split.
   * 
   * @param tree Tree to use
   * @param node Node to split
   * @return the assignments of this split
   */
  abstract public Assignments<E> split(AbstractMTree<?, N, E, ?> tree, N node);
}
