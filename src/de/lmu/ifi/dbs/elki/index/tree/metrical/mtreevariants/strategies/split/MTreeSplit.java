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
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
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
public abstract class MTreeSplit<O, D extends NumberDistance<D, ?>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry> {
  /**
   * Compute the pairwise distances in the given node.
   * 
   * @param tree Tree
   * @param node Node
   * @return Distance matrix
   */
  protected double[] computeDistanceMatrix(AbstractMTree<O, D, N, E, ?> tree, N node) {
    final int n = node.getNumEntries();
    double[] distancematrix = new double[n * n];
    // Build distance matrix
    for (int i = 0; i < n; i++) {
      E ei = node.getEntry(i);
      for (int j = 0; j < n; j++) {
        if (i == j) {
          distancematrix[i * n + j] = 0.0;
        } else if (i < j) {
          distancematrix[i * n + j] = tree.distance(ei, node.getEntry(j)).doubleValue();
        } else { // i > j
          distancematrix[i * n + j] = distancematrix[j * n + i];
        }
      }
    }
    return distancematrix;
  }

  /**
   * Creates a balanced partition of the entries of the specified node.
   * 
   * @param tree the tree to perform the split in
   * @param node the node to be split
   * @param routingObject1 the id of the first routing object
   * @param routingObject2 the id of the second routing object
   * @return an assignment that holds a balanced partition of the entries of the
   *         specified node
   */
  Assignments<E> balancedPartition(AbstractMTree<O, D, N, E, ?> tree, N node, DBID routingObject1, DBID routingObject2) {
    BitSet assigned = new BitSet(node.getNumEntries());
    List<DistanceEntry<E>> assigned1 = new ArrayList<>(node.getCapacity());
    List<DistanceEntry<E>> assigned2 = new ArrayList<>(node.getCapacity());

    double currentCR1 = 0.;
    double currentCR2 = 0.;

    List<DistanceEntry<E>> list1 = new ArrayList<>();
    List<DistanceEntry<E>> list2 = new ArrayList<>();

    // determine the nearest neighbors
    for (int i = 0; i < node.getNumEntries(); i++) {
      final E ent = node.getEntry(i);
      DBID id = ent.getRoutingObjectID();
      if (DBIDUtil.equal(id, routingObject1)) {
        assigned1.add(new DistanceEntry<>(ent, 0., i));
        continue;
      }
      if (DBIDUtil.equal(id, routingObject2)) {
        assigned2.add(new DistanceEntry<>(ent, 0., i));
        continue;
      }
      // determine the distance of o to o1 / o2
      double d1 = tree.distance(routingObject1, id).doubleValue();
      double d2 = tree.distance(routingObject2, id).doubleValue();

      list1.add(new DistanceEntry<>(ent, d1, i));
      list2.add(new DistanceEntry<>(ent, d2, i));
    }
    Collections.sort(list1, Collections.reverseOrder());
    Collections.sort(list2, Collections.reverseOrder());

    for (int i = 2; i < node.getNumEntries(); i++) {
      currentCR1 = assignNN(assigned, assigned1, list1, currentCR1, node.isLeaf());
      i++;
      if (i < node.getNumEntries()) {
        currentCR2 = assignNN(assigned, assigned2, list2, currentCR2, node.isLeaf());
      }
    }
    return new Assignments<>(routingObject1, routingObject2, currentCR1, currentCR2, assigned1, assigned2);
  }

  /**
   * Creates a balanced partition of the entries of the specified node.
   * 
   * @param tree the tree to perform the split in
   * @param node the node to be split
   * @param routingEntNum1 the entry number of the first routing object
   * @param routingEntNum2 the entry number of the second routing object
   * @param distanceMatrix precomputed distance matrix to use
   * @return an assignment that holds a balanced partition of the entries of the
   *         specified node
   */
  Assignments<E> balancedPartition(AbstractMTree<O, D, N, E, ?> tree, N node, int routingEntNum1, int routingEntNum2, double[] distanceMatrix) {
    final int n = node.getNumEntries();
    BitSet assigned = new BitSet(node.getNumEntries());
    List<DistanceEntry<E>> assigned1 = new ArrayList<>(node.getCapacity());
    List<DistanceEntry<E>> assigned2 = new ArrayList<>(node.getCapacity());

    double currentCR1 = 0.;
    double currentCR2 = 0.;

    List<DistanceEntry<E>> list1 = new ArrayList<>();
    List<DistanceEntry<E>> list2 = new ArrayList<>();

    DBID routingObject1 = null, routingObject2 = null;
    // determine the nearest neighbors
    for (int i = 0; i < node.getNumEntries(); i++) {
      final E ent = node.getEntry(i);
      if (i == routingEntNum1) {
        routingObject1 = ent.getRoutingObjectID();
        assigned1.add(new DistanceEntry<>(ent, 0., i));
        continue;
      }
      if (i == routingEntNum2) {
        routingObject2 = ent.getRoutingObjectID();
        assigned2.add(new DistanceEntry<>(ent, 0., i));
        continue;
      }
      // Look up the distances of o to o1 / o2
      double d1 = distanceMatrix[i * n + routingEntNum1];
      double d2 = distanceMatrix[i * n + routingEntNum2];

      list1.add(new DistanceEntry<>(ent, d1, i));
      list2.add(new DistanceEntry<>(ent, d2, i));
    }
    Collections.sort(list1, Collections.reverseOrder());
    Collections.sort(list2, Collections.reverseOrder());

    for (int i = 2; i < node.getNumEntries(); i++) {
      currentCR1 = assignNN(assigned, assigned1, list1, currentCR1, node.isLeaf());
      i++;
      if (i < node.getNumEntries()) {
        currentCR2 = assignNN(assigned, assigned2, list2, currentCR2, node.isLeaf());
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
   * @param list the list, the first object should be assigned
   * @param currentCR the current covering radius
   * @param isLeaf true, if the node of the entries to be assigned is a leaf,
   *        false otherwise
   * @return the new covering radius
   */
  private double assignNN(BitSet assigned, List<DistanceEntry<E>> assigned1, List<DistanceEntry<E>> list, double currentCR, boolean isLeaf) {
    // Remove last unassigned:
    DistanceEntry<E> distEntry = list.remove(list.size() - 1);
    while (assigned.get(distEntry.getIndex())) {
      distEntry = list.remove(list.size() - 1);
    }
    assigned1.add(distEntry);
    assigned.set(distEntry.getIndex());

    if (isLeaf) {
      return Math.max(currentCR, distEntry.getDistance());
    } else {
      return Math.max(currentCR, distEntry.getDistance() + (distEntry.getEntry()).getCoveringRadius());
    }
  }

  /**
   * Returns the assignments of this split.
   * 
   * @param tree Tree to use
   * @param node Node to split
   * @return the assignments of this split
   */
  abstract public Assignments<E> split(AbstractMTree<O, D, N, E, ?> tree, N node);
}
