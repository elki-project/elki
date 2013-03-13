package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.insert;

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
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexPathComponent;
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
 */
@Reference(authors = "P. Ciaccia, M. Patella, P. Zezula", title = "M-tree: An Efficient Access Method for Similarity Search in Metric Spaces", booktitle = "VLDB'97, Proceedings of 23rd International Conference on Very Large Data Bases, August 25-29, 1997, Athens, Greece", url = "http://www.vldb.org/conf/1997/P426.PDF")
public class MinimumEnlargementInsert<O, D extends NumberDistance<D, ?>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry> implements MTreeInsert<O, D, N, E> {
  @Override
  public IndexTreePath<E> choosePath(AbstractMTree<O, D, N, E, ?> tree, E object) {
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
  private IndexTreePath<E> choosePath(AbstractMTree<O, D, N, E, ?> tree, E object, IndexTreePath<E> subtree) {
    N node = tree.getNode(subtree.getLastPathComponent().getEntry());

    // leaf
    if (node.isLeaf()) {
      return subtree;
    }

    double bestDistance;
    int bestIdx;
    E bestEntry;
    double enlarge; // Track best enlargement - null for no enlargement needed.
    // Initialize from first:
    {
      bestIdx = 0;
      bestEntry = node.getEntry(0);
      bestDistance = tree.distance(object.getRoutingObjectID(), bestEntry.getRoutingObjectID()).doubleValue();
      if (bestDistance <= bestEntry.getCoveringRadius()) {
        enlarge = 0.;
      } else {
        enlarge = bestDistance - bestEntry.getCoveringRadius();
      }
    }

    // Iterate over remaining
    for (int i = 1; i < node.getNumEntries(); i++) {
      E entry = node.getEntry(i);
      double distance = tree.distance(object.getRoutingObjectID(), entry.getRoutingObjectID()).doubleValue();

      if (distance <= entry.getCoveringRadius()) {
        if (enlarge > 0. || distance < bestDistance) {
          bestIdx = i;
          bestEntry = entry;
          bestDistance = distance;
          enlarge = 0.;
        }
      } else if (enlarge > 0.) {
        double enlrg = distance - entry.getCoveringRadius();
        if (enlrg < enlarge) {
          bestIdx = i;
          bestEntry = entry;
          bestDistance = distance;
          enlarge = enlrg;
        }
      }
    }

    // FIXME: move this to the actual insertion procedure!
    // Apply enlargement
    if (enlarge > 0) {
      bestEntry.setCoveringRadius(bestEntry.getCoveringRadius() + enlarge);
    }

    return choosePath(tree, object, subtree.pathByAddingChild(new TreeIndexPathComponent<>(bestEntry, bestIdx)));
  }
}
