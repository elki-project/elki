package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Encapsulates the required methods for a split of a node in an M-Tree. The
 * routing objects are chosen according to the M_LB_DIST strategy.
 * 
 * Reference:
 * <p>
 * P. Ciaccia, M. Patella, P. Zezula<br />
 * M-tree: An Efficient Access Method for Similarity Search in Metric Spaces<br />
 * In Proceedings of 23rd International Conference on Very Large Data Bases
 * (VLDB'97), August 25-29, 1997, Athens, Greece
 * </p>
 * 
 * @author Elke Achtert
 * 
 * @param <O> the type of DatabaseObject to be stored in the M-Tree
 * @param <N> the type of AbstractMTreeNode used in the M-Tree
 * @param <E> the type of MetricalEntry used in the M-Tree
 */
@Reference(authors = "P. Ciaccia, M. Patella, P. Zezula", //
title = "M-tree: An Efficient Access Method for Similarity Search in Metric Spaces", //
booktitle = "VLDB'97, Proceedings of 23rd International Conference on Very Large Data Bases, August 25-29, 1997, Athens, Greece", //
url = "http://www.vldb.org/conf/1997/P426.PDF")
public class MLBDistSplit<O, N extends AbstractMTreeNode<O, N, E>, E extends MTreeEntry> extends MTreeSplit<O, N, E> {
  /**
   * Creates a new split object.
   */
  public MLBDistSplit() {
    super();
  }

  /**
   * Selects the second object of the specified node to be promoted and stored
   * into the parent node and partitions the entries according to the M_LB_DIST
   * strategy.
   * <p/>
   * This strategy considers all possible pairs of objects and chooses the pair
   * of objects for which the distance is maximum.
   * 
   * @param tree Tree to use
   * @param node the node to be split
   */
  @Override
  public Assignments<E> split(AbstractMTree<O, N, E, ?> tree, N node) {
    final int n = node.getNumEntries();
    double[] distanceMatrix = computeDistanceMatrix(tree, node);

    // choose first and second routing object
    int besti = -1, bestj = -1;
    double currentMaxDist = Double.NEGATIVE_INFINITY;
    for(int i = 0; i < n; i++) {
      int row = i * n;
      for(int j = i + 1; j < n; j++) {
        double distance = distanceMatrix[row + j];
        if(distance > currentMaxDist) {
          besti = i;
          bestj = j;
          currentMaxDist = distance;
        }
      }
    }

    return balancedPartition(tree, node, besti, bestj, distanceMatrix);
  }
}
