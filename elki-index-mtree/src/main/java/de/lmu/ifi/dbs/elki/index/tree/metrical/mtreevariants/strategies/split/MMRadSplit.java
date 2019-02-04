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

import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.distribution.Assignments;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.distribution.DistributionStrategy;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Encapsulates the required methods for a split of a node in an M-Tree. The
 * routing objects are chosen according to the mMrad strategy.
 * <p>
 * Reference:
 * <p>
 * P. Ciaccia, M. Patella, P. Zezula<br>
 * M-tree: An Efficient Access Method for Similarity Search in Metric Spaces<br>
 * Proc. Int. Conf. Very Large Data Bases (VLDB'97)
 *
 * @author Elke Achtert
 * @since 0.2
 *
 * @param <E> the type of MTreeEntry used in the M-Tree
 * @param <N> the type of AbstractMTreeNode used in the M-Tree
 */
@Reference(authors = "P. Ciaccia, M. Patella, P. Zezula", //
    title = "M-tree: An Efficient Access Method for Similarity Search in Metric Spaces", //
    booktitle = "Proc. Int. Conf. Very Large Data Bases (VLDB'97)", //
    url = "http://www.vldb.org/conf/1997/P426.PDF", //
    bibkey = "DBLP:conf/vldb/CiacciaPZ97")
public class MMRadSplit<E extends MTreeEntry, N extends AbstractMTreeNode<?, N, E>> extends AbstractMTreeSplit<E, N> {
  /**
   * Constructor.
   *
   * @param distributor Distribution strategy
   */
  public MMRadSplit(DistributionStrategy distributor) {
    super(distributor);
  }

  /**
   * Selects two objects of the specified node to be promoted and stored into
   * the parent node. The mM-RAD strategy considers all possible pairs of
   * objects and, after partitioning the set of entries, promotes the pair of
   * objects for which the larger of the two covering radiuses is minimum.
   * 
   * @param tree Tree to use
   * @param node the node to be split
   */
  @Override
  public Assignments<E> split(AbstractMTree<?, N, E, ?> tree, N node) {
    final int n = node.getNumEntries();
    double[][] distanceMatrix = computeDistanceMatrix(tree, node);

    double miSumCR = Double.POSITIVE_INFINITY;
    boolean leaf = node.isLeaf();
    Assignments<E> bestAssignment = null;
    for(int i = 0; i < n; i++) {
      for(int j = i + 1; j < n; j++) {
        Assignments<E> currentAssignments = distributor.distribute(node, i, distanceMatrix[i], j, distanceMatrix[j]);

        double maxCR = Math.max(currentAssignments.computeFirstCover(leaf), currentAssignments.computeSecondCover(leaf));
        if(maxCR < miSumCR) {
          miSumCR = maxCR;
          bestAssignment = currentAssignments;
        }
      }
    }
    return bestAssignment;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <E> the type of MTreeEntry used in the M-Tree
   * @param <N> the type of AbstractMTreeNode used in the M-Tree
   */
  public static class Parameterizer<E extends MTreeEntry, N extends AbstractMTreeNode<?, N, E>> extends AbstractMTreeSplit.Parameterizer<E, N> {
    @Override
    protected MMRadSplit<E, N> makeInstance() {
      return new MMRadSplit<>(distributor);
    }
  }
}
