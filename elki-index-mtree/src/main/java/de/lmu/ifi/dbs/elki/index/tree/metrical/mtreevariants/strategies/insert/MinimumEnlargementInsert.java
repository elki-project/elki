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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.insert;

import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Minimum enlargement insert - default insertion strategy for the M-tree.
 * <p>
 * Reference:
 * <p>
 * P. Ciaccia, M. Patella, P. Zezula<br>
 * M-tree: An Efficient Access Method for Similarity Search in Metric Spaces<br>
 * In Proc. Int. Conf. Very Large Data Bases (VLDB'97)
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "P. Ciaccia, M. Patella, P. Zezula", //
    title = "M-tree: An Efficient Access Method for Similarity Search in Metric Spaces", //
    booktitle = "Proc. Int. Conf. Very Large Data Bases (VLDB'97)", //
    url = "http://www.vldb.org/conf/1997/P426.PDF", //
    bibkey = "DBLP:conf/vldb/CiacciaPZ97")
public class MinimumEnlargementInsert<N extends AbstractMTreeNode<?, N, E>, E extends MTreeEntry> implements MTreeInsert<E, N> {
  @Override
  public IndexTreePath<E> choosePath(AbstractMTree<?, N, E, ?> tree, E object) {
    return choosePath(tree, object, tree.getRootPath());
  }

  /**
   * Chooses the best path of the specified subtree for insertion of the given
   * object.
   * 
   * @param tree the tree to insert into
   * @param object the entry to search
   * @param subtree the subtree to be tested for insertion
   * @return the path of the appropriate subtree to insert the given object
   */
  private IndexTreePath<E> choosePath(AbstractMTree<?, N, E, ?> tree, E object, IndexTreePath<E> subtree) {
    N node = tree.getNode(subtree.getEntry());

    // leaf
    if(node.isLeaf()) {
      return subtree;
    }

    // Initialize from first:
    int bestIdx = 0;
    E bestEntry = node.getEntry(0);
    double bestDistance = tree.distance(object.getRoutingObjectID(), bestEntry.getRoutingObjectID());

    // Iterate over remaining
    for(int i = 1; i < node.getNumEntries(); i++) {
      E entry = node.getEntry(i);
      double distance = tree.distance(object.getRoutingObjectID(), entry.getRoutingObjectID());

      if(distance < bestDistance) {
        bestIdx = i;
        bestEntry = entry;
        bestDistance = distance;
      }
    }
    return choosePath(tree, object, new IndexTreePath<>(subtree, bestEntry, bestIdx));
  }
}
