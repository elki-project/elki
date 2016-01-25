package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split;

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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;

/**
 * Abstract super class for splitting a node in an M-Tree.
 * 
 * @author Elke Achtert
 * @since 0.2
 * 
 * @apiviz.composedOf Assignments
 * 
 * @param <O> the type of DatabaseObject to be stored in the M-Tree
 * @param <N> the type of AbstractMTreeNode used in the M-Tree
 * @param <E> the type of MetricalEntry used in the M-Tree
 */
public abstract class MTreeSplit<O, N extends AbstractMTreeNode<O, N, E>, E extends MTreeEntry> {
  /**
   * Compute the pairwise distances in the given node.
   * 
   * @param tree Tree
   * @param node Node
   * @return Distance matrix
   */
  protected double[] computeDistanceMatrix(AbstractMTree<O, N, E, ?> tree, N node) {
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
   * Creates a balanced partition of the entries of the specified node.
   * 
   * @param tree the tree to perform the split in
   * @param node the node to be split
   * @param routingObject1 the id of the first routing object
   * @param routingObject2 the id of the second routing object
   * @return an assignment that holds a balanced partition of the entries of the
   *         specified node
   */
  Assignments<E> balancedPartition(AbstractMTree<O, N, E, ?> tree, N node, DBID routingObject1, DBID routingObject2) {
    assert (routingObject1 != null && routingObject2 != null);
    final int n = node.getNumEntries(), l = n - 2;
    int[] idx1 = new int[l], idx2 = new int[l];
    double[] dis1 = new double[l], dis2 = new double[l];

    Assignments<E> assign = new Assignments<>((n + 1) >> 1);
    assign.setFirstRoutingObject(routingObject1);
    assign.setSecondRoutingObject(routingObject2);
    for(int i = 0, j = 0; i < n; i++) {
      final E ent = node.getEntry(i);
      final DBID id = ent.getRoutingObjectID();
      if(DBIDUtil.equal(id, routingObject1)) {
        assign.addToFirst(ent, 0., i);
        continue;
      }
      if(DBIDUtil.equal(id, routingObject2)) {
        assign.addToSecond(ent, 0., i);
        continue;
      }
      // determine the distance of o to o1 / o2
      dis1[j] = tree.distance(routingObject1, id);
      idx1[j] = i;
      dis2[j] = tree.distance(routingObject2, id);
      idx2[j] = i;
      j++;
    }
    DoubleIntegerArrayQuickSort.sort(dis1, idx1, l);
    DoubleIntegerArrayQuickSort.sort(dis2, idx2, l);

    long[] assigned = BitsUtil.zero(n);
    int p1 = 0, p2 = 0;
    for(int i = 0; i < l; ++i) {
      p1 = assignBest(assign, assigned, node, dis1, idx1, p1, false);
      if(++i < l) {
        p2 = assignBest(assign, assigned, node, dis2, idx2, p2, true);
      }
    }
    assert (assign.getFirstAssignments().size() + assign.getSecondAssignments().size() == n) : "Sizes do not sum up: " + assign.getFirstAssignments().size() + " + " + assign.getFirstAssignments().size() + " != " + n;
    assert (Math.abs(assign.getFirstAssignments().size() - assign.getSecondAssignments().size()) <= 1) : assign.getFirstAssignments().size() + " " + assign.getSecondAssignments().size();
    return assign;
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
  Assignments<E> balancedPartition(AbstractMTree<O, N, E, ?> tree, N node, int routingEntNum1, int routingEntNum2, double[] distanceMatrix) {
    final int n = node.getNumEntries(), l = n - 2;
    int[] idx1 = new int[l], idx2 = new int[l];
    double[] dis1 = new double[l], dis2 = new double[l];

    Assignments<E> assign = new Assignments<>((n + 1) >> 1);
    // determine the nearest neighbors
    for(int i = 0, j = 0; i < node.getNumEntries(); i++) {
      final E ent = node.getEntry(i);
      if(i == routingEntNum1) {
        assign.setFirstRoutingObject(ent.getRoutingObjectID());
        assign.addToFirst(ent, 0., i);
        continue;
      }
      if(i == routingEntNum2) {
        assign.setSecondRoutingObject(ent.getRoutingObjectID());
        assign.addToSecond(ent, 0., i);
        continue;
      }
      // Look up the distances of o to o1 / o2
      dis1[j] = distanceMatrix[i * n + routingEntNum1];
      idx1[j] = i;
      dis2[j] = distanceMatrix[i * n + routingEntNum2];
      idx2[j] = i;
      j++;
    }
    DoubleIntegerArrayQuickSort.sort(dis1, idx1, l);
    DoubleIntegerArrayQuickSort.sort(dis2, idx2, l);

    long[] assigned = BitsUtil.zero(n);
    int p1 = 0, p2 = 0;
    for(int i = 0; i < l; ++i) {
      p1 = assignBest(assign, assigned, node, dis1, idx1, p1, false);
      if(++i < l) {
        p2 = assignBest(assign, assigned, node, dis2, idx2, p2, true);
      }
    }
    assert (assign.getFirstAssignments().size() + assign.getSecondAssignments().size() == n) : "Sizes do not sum up: " + assign.getFirstAssignments().size() + " + " + assign.getFirstAssignments().size() + " != " + n;
    assert (Math.abs(assign.getFirstAssignments().size() - assign.getSecondAssignments().size()) <= 1) : assign.getFirstAssignments().size() + " " + assign.getSecondAssignments().size();
    return assign;
  }

  /**
   * Assigns the first object of the specified list to the first assignment that
   * it is not yet assigned to the second assignment.
   * 
   * @param assign Output assignment
   * @param assigned Bitset of assigned objects
   * @param dis Distances
   * @param idx Indexes
   * @param pos Current position
   * @param second Assign to second, not first, set.
   * @return the new index
   */
  private int assignBest(Assignments<E> assign, long[] assigned, N node, double[] dis, int[] idx, int pos, boolean second) {
    int i = idx[pos];
    // Skip already assigned objects:
    while(BitsUtil.get(assigned, i)) {
      i = idx[++pos];
    }
    if(second) {
      assign.addToSecond(node.getEntry(i), dis[pos], i);
    }
    else {
      assign.addToFirst(node.getEntry(i), dis[pos], i);
    }
    BitsUtil.setI(assigned, i);
    return ++pos;
  }

  /**
   * Returns the assignments of this split.
   * 
   * @param tree Tree to use
   * @param node Node to split
   * @return the assignments of this split
   */
  abstract public Assignments<E> split(AbstractMTree<O, N, E, ?> tree, N node);
}
