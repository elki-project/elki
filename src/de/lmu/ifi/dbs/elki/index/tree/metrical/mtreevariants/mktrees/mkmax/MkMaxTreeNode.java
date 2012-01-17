package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;

/**
 * Represents a node in an {@link MkMaxTree}.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has MkMaxEntry oneway - - contains
 * 
 * @param <O> the type of DatabaseObject to be stored in the MkMaxTree
 * @param <D> the type of Distance used in the MkMaxTree
 */
class MkMaxTreeNode<O, D extends Distance<D>> extends AbstractMTreeNode<O, D, MkMaxTreeNode<O, D>, MkMaxEntry<D>> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1;

  /**
   * Empty constructor for Externalizable interface.
   */
  public MkMaxTreeNode() {
    // empty constructor
  }

  /**
   * Creates a new MkMaxTreeNode object.
   * 
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public MkMaxTreeNode(int capacity, boolean isLeaf) {
    super(capacity, isLeaf, MkMaxEntry.class);
  }

  /**
   * Determines and returns the k-nearest neighbor distance of this node as the
   * maximum of the k-nearest neighbor distances of all entries.
   * 
   * @param distanceFunction the distance function
   * @return the knn distance of this node
   */
  protected D kNNDistance(DistanceQuery<O, D> distanceFunction) {
    D knnDist = distanceFunction.nullDistance();
    for(int i = 0; i < getNumEntries(); i++) {
      MkMaxEntry<D> entry = getEntry(i);
      knnDist = DistanceUtil.max(knnDist, entry.getKnnDistance());
    }
    return knnDist;
  }

  /**
   * Calls the super method and adjust additionally the k-nearest neighbor
   * distance of this node as the maximum of the k-nearest neighbor distances of
   * all its entries.
   */
  @Override
  public void adjustEntry(MkMaxEntry<D> entry, DBID routingObjectID, D parentDistance, AbstractMTree<O, D, MkMaxTreeNode<O, D>, MkMaxEntry<D>> mTree) {
    super.adjustEntry(entry, routingObjectID, parentDistance, mTree);
    // adjust knn distance
    entry.setKnnDistance(kNNDistance(mTree.getDistanceQuery()));
  }

  /**
   * Calls the super method and tests if the k-nearest neighbor distance of this
   * node is correctly set.
   */
  @Override
  protected void integrityCheckParameters(MkMaxEntry<D> parentEntry, MkMaxTreeNode<O, D> parent, int index, AbstractMTree<O, D, MkMaxTreeNode<O, D>, MkMaxEntry<D>> mTree) {
    super.integrityCheckParameters(parentEntry, parent, index, mTree);
    // test if knn distance is correctly set
    MkMaxEntry<D> entry = parent.getEntry(index);
    D knnDistance = kNNDistance(mTree.getDistanceQuery());
    if(!entry.getKnnDistance().equals(knnDistance)) {
      String soll = knnDistance.toString();
      String ist = entry.getKnnDistance().toString();
      throw new RuntimeException("Wrong knnDistance in node " + parent.getPageID() + " at index " + index + " (child " + entry + ")" + "\nsoll: " + soll + ",\n ist: " + ist);
    }
  }
}
