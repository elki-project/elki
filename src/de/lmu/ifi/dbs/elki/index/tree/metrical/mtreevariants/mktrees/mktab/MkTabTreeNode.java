package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mktab;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;

/**
 * Represents a node in a MkMax-Tree.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has MkTabEntry oneway - - contains
 * 
 * @param <O> object type
 * @param <D> distance type
 */
class MkTabTreeNode<O, D extends Distance<D>> extends AbstractMTreeNode<O, D, MkTabTreeNode<O, D>, MkTabEntry<D>> {
  private static final long serialVersionUID = 1;

  /**
   * Empty constructor for Externalizable interface.
   */
  public MkTabTreeNode() {
    // empty constructor
  }

  /**
   * Creates a MkTabTreeNode object.
   * 
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public MkTabTreeNode(int capacity, boolean isLeaf) {
    super(capacity, isLeaf, MkTabEntry.class);
  }

  /**
   * Determines and returns the knn distance of this node as the maximum knn
   * distance of all entries.
   * 
   * @param distanceFunction the distance function
   * @return the knn distance of this node
   */
  protected List<D> kNNDistances(DistanceQuery<O, D> distanceFunction) {
    int k = getEntry(0).getK_max();

    List<D> result = new ArrayList<D>();
    for(int i = 0; i < k; i++) {
      result.add(distanceFunction.nullDistance());
    }

    for(int i = 0; i < getNumEntries(); i++) {
      for(int j = 0; j < k; j++) {
        MkTabEntry<D> entry = getEntry(i);
        D kDist = result.remove(j);
        result.add(j, DistanceUtil.max(kDist, entry.getKnnDistance(j + 1)));
      }
    }

    return result;
  }

  @Override
  public void adjustEntry(MkTabEntry<D> entry, DBID routingObjectID, D parentDistance, AbstractMTree<O, D, MkTabTreeNode<O, D>, MkTabEntry<D>> mTree) {
    super.adjustEntry(entry, routingObjectID, parentDistance, mTree);
    // adjust knn distances
    entry.setKnnDistances(kNNDistances(mTree.getDistanceQuery()));
  }

  /**
   * Tests, if the parameters of the entry representing this node, are correctly
   * set. Subclasses may need to overwrite this method.
   * 
   * @param parent the parent holding the entry representing this node
   * @param index the index of the entry in the parents child array
   * @param mTree the underlying M-Tree
   */
  @Override
  protected void integrityCheckParameters(MkTabEntry<D> parentEntry, MkTabTreeNode<O, D> parent, int index, AbstractMTree<O, D, MkTabTreeNode<O, D>, MkTabEntry<D>> mTree) {
    super.integrityCheckParameters(parentEntry, parent, index, mTree);
    // test knn distances
    MkTabEntry<D> entry = parent.getEntry(index);
    List<D> knnDistances = kNNDistances(mTree.getDistanceQuery());
    if(!entry.getKnnDistances().equals(knnDistances)) {
      String soll = knnDistances.toString();
      String ist = entry.getKnnDistances().toString();
      throw new RuntimeException("Wrong knnDistances in node " + parent.getPageID() + " at index " + index + " (child " + entry + ")" + "\nsoll: " + soll + ",\n ist: " + ist);
    }
  }
}
