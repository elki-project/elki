package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split;

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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * Abstract super class for splitting a node in an M-Tree.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.composedOf Assignments
 * 
 * @param <O> the type of DatabaseObject to be stored in the M-Tree
 * @param <D> the type of Distance used in the M-Tree
 * @param <N> the type of AbstractMTreeNode used in the M-Tree
 * @param <E> the type of MetricalEntry used in the M-Tree
 */
public abstract class MTreeSplit<O, D extends Distance<D>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry<D>> {
  /**
   * Creates a balanced partition of the entries of the specified node.
   * 
   * @param node the node to be split
   * @param routingObject1 the id of the first routing object
   * @param routingObject2 the id of the second routing object
   * @param distanceFunction the distance function to compute the distances
   * @return an assignment that holds a balanced partition of the entries of the
   *         specified node
   */
  Assignments<D, E> balancedPartition(AbstractMTree<O, D, N, E> tree, N node, DBID routingObject1, DBID routingObject2) {
    BitSet assigned = new BitSet(node.getNumEntries());
    List<E> assigned1 = new ArrayList<>(node.getCapacity());
    List<E> assigned2 = new ArrayList<>(node.getCapacity());

    D currentCR1 = tree.getDistanceFactory().nullDistance();
    D currentCR2 = tree.getDistanceFactory().nullDistance();

    // determine the nearest neighbors
    List<DistanceEntry<D, E>> list1 = new ArrayList<>();
    List<DistanceEntry<D, E>> list2 = new ArrayList<>();
    for (int i = 0; i < node.getNumEntries(); i++) {
      final E ent = node.getEntry(i);
      DBID id = ent.getRoutingObjectID();
      if (DBIDUtil.equal(id, routingObject1)) {
        ent.setParentDistance(tree.getDistanceFactory().nullDistance());
        assigned1.add(ent);
        continue;
      }
      if (DBIDUtil.equal(id, routingObject2)) {
        ent.setParentDistance(tree.getDistanceFactory().nullDistance());
        assigned2.add(ent);
        continue;
      }
      // determine the distance of o to o1 / o2
      D d1 = tree.distance(routingObject1, id);
      D d2 = tree.distance(routingObject2, id);

      list1.add(new DistanceEntry<>(ent, d1, i));
      list2.add(new DistanceEntry<>(ent, d2, i));
    }
    Collections.sort(list1, Collections.reverseOrder());
    Collections.sort(list2, Collections.reverseOrder());

    for (int i = 0; i < node.getNumEntries(); i++) {
      currentCR1 = assignNN(assigned, assigned1, assigned2, list1, currentCR1, node.isLeaf());
      i++;
      if (i < node.getNumEntries()) {
        currentCR2 = assignNN(assigned, assigned2, assigned1, list2, currentCR2, node.isLeaf());
      }
    }
    return new Assignments<>(routingObject1, routingObject2, currentCR1, currentCR2, assigned1, assigned2);
  }

  /**
   * Assigns the first object of the specified list to the first assignment that
   * it is not yet assigned to the second assignment.
   * 
   * @param assigned List of already assigned objects
   * @param assigned1 the first assignment
   * @param assigned2 the second assignment
   * @param list the list, the first object should be assigned
   * @param currentCR the current covering radius
   * @param isLeaf true, if the node of the entries to be assigned is a leaf,
   *        false otherwise
   * @return the new covering radius
   */
  private D assignNN(BitSet assigned, List<E> assigned1, List<E> assigned2, List<DistanceEntry<D, E>> list, D currentCR, boolean isLeaf) {
    DistanceEntry<D, E> distEntry = list.remove(list.size() - 1);
    while (assigned.get(distEntry.getIndex())) {
      distEntry = list.remove(list.size() - 1);
    }
    // Update the parent distance.
    distEntry.getEntry().setParentDistance(distEntry.getDistance());
    assigned1.add(distEntry.getEntry());
    assigned.set(distEntry.getIndex());

    if (isLeaf) {
      return DistanceUtil.max(currentCR, distEntry.getDistance());
    } else {
      return DistanceUtil.max(currentCR, distEntry.getDistance().plus((distEntry.getEntry()).getCoveringRadius()));
    }
  }

  /**
   * Returns the assignments of this split.
   * 
   * @param tree Tree to use
   * @param node Node to split
   * @return the assignments of this split
   */
  abstract public Assignments<D, E> split(AbstractMTree<O, D, N, E> tree, N node);
}
