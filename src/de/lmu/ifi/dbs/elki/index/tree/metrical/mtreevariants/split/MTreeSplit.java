package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.split;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.DistanceEntry;
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
   * Encapsulates the two promotion objects and their assignments.
   */
  Assignments<D, E> assignments;

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
  Assignments<D, E> balancedPartition(N node, DBID routingObject1, DBID routingObject2, DistanceQuery<O, D> distanceFunction) {
    HashSet<E> assigned1 = new HashSet<E>();
    HashSet<E> assigned2 = new HashSet<E>();

    D currentCR1 = distanceFunction.nullDistance();
    D currentCR2 = distanceFunction.nullDistance();

    // determine the nearest neighbors
    List<DistanceEntry<D, E>> list1 = new ArrayList<DistanceEntry<D, E>>();
    List<DistanceEntry<D, E>> list2 = new ArrayList<DistanceEntry<D, E>>();
    for(int i = 0; i < node.getNumEntries(); i++) {
      DBID id = node.getEntry(i).getRoutingObjectID();
      // determine the distance of o to o1 / o2
      D d1 = distanceFunction.distance(routingObject1, id);
      D d2 = distanceFunction.distance(routingObject2, id);

      list1.add(new DistanceEntry<D, E>(node.getEntry(i), d1, i));
      list2.add(new DistanceEntry<D, E>(node.getEntry(i), d2, i));
    }
    Collections.sort(list1);
    Collections.sort(list2);

    for(int i = 0; i < node.getNumEntries(); i++) {
      if(i % 2 == 0) {
        currentCR1 = assignNN(assigned1, assigned2, list1, currentCR1, node.isLeaf());
      }
      else {
        currentCR2 = assignNN(assigned2, assigned1, list2, currentCR2, node.isLeaf());
      }
    }
    return new Assignments<D, E>(routingObject1, routingObject2, currentCR1, currentCR2, assigned1, assigned2);
  }

  /**
   * Assigns the first object of the specified list to the first assignment that
   * it is not yet assigned to the second assignment.
   * 
   * @param assigned1 the first assignment
   * @param assigned2 the second assignment
   * @param list the list, the first object should be assigned
   * @param currentCR the current covering radius
   * @param isLeaf true, if the node of the entries to be assigned is a leaf,
   *        false otherwise
   * @return the new covering radius
   */
  private D assignNN(Set<E> assigned1, Set<E> assigned2, List<DistanceEntry<D, E>> list, D currentCR, boolean isLeaf) {
    DistanceEntry<D, E> distEntry = list.remove(0);
    while(assigned2.contains(distEntry.getEntry())) {
      distEntry = list.remove(0);
    }
    // Update the parent distance.
    distEntry.getEntry().setParentDistance(distEntry.getDistance());
    assigned1.add(distEntry.getEntry());

    if(isLeaf) {
      return DistanceUtil.max(currentCR, distEntry.getDistance());
    }
    else {
      return DistanceUtil.max(currentCR, distEntry.getDistance().plus((distEntry.getEntry()).getCoveringRadius()));
    }
  }

  /**
   * Returns the assignments of this split.
   * 
   * @return the assignments of this split
   */
  public Assignments<D, E> getAssignments() {
    return assignments;
  }
}
