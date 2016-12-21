package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.insert;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Default insertion strategy for the M-tree.
 * 
 * Reference:
 * <p>
 * P. Ciaccia, M. Patella, P. Zezula<br />
 * M-tree: An Efficient Access Method for Similarity Search in Metric Spaces<br />
 * In Proceedings of 23rd International Conference on Very Large Data Bases
 * (VLDB'97), August 25-29, 1997, Athens, Greece
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "P. Ciaccia, M. Patella, P. Zezula", //
title = "M-tree: An Efficient Access Method for Similarity Search in Metric Spaces", //
booktitle = "VLDB'97, Proceedings of 23rd International Conference on Very Large Data Bases, August 25-29, 1997, Athens, Greece", //
url = "http://www.vldb.org/conf/1997/P426.PDF")
public class MinimumEnlargementInsert<O, N extends AbstractMTreeNode<O, N, E>, E extends MTreeEntry> implements MTreeInsert<O, N, E> {
  @Override
  public IndexTreePath<E> choosePath(AbstractMTree<O, N, E, ?> tree, E object) {
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
  private IndexTreePath<E> choosePath(AbstractMTree<O, N, E, ?> tree, E object, IndexTreePath<E> subtree) {
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
