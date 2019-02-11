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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;

/**
 * Represents a node in an {@link MkMaxTree}.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @navhas - contains - MkMaxEntry
 * 
 * @param <O> the type of DatabaseObject to be stored in the MkMaxTree
 */
class MkMaxTreeNode<O> extends AbstractMTreeNode<O, MkMaxTreeNode<O>, MkMaxEntry> {
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
    super(capacity, isLeaf);
  }

  /**
   * Determines and returns the k-nearest neighbor distance of this node as the
   * maximum of the k-nearest neighbor distances of all entries.
   * 
   * @return the knn distance of this node
   */
  protected double kNNDistance() {
    double knnDist = 0.;
    for(int i = 0; i < getNumEntries(); i++) {
      MkMaxEntry entry = getEntry(i);
      knnDist = Math.max(knnDist, entry.getKnnDistance());
    }
    return knnDist;
  }

  /**
   * Calls the super method and adjust additionally the k-nearest neighbor
   * distance of this node as the maximum of the k-nearest neighbor distances of
   * all its entries.
   */
  @Override
  public boolean adjustEntry(MkMaxEntry entry, DBID routingObjectID, double parentDistance, AbstractMTree<O, MkMaxTreeNode<O>, MkMaxEntry, ?> mTree) {
    super.adjustEntry(entry, routingObjectID, parentDistance, mTree);
    // adjust knn distance
    entry.setKnnDistance(kNNDistance());
    return true; // TODO: improve
  }

  /**
   * Calls the super method and tests if the k-nearest neighbor distance of this
   * node is correctly set.
   */
  @Override
  protected void integrityCheckParameters(MkMaxEntry parentEntry, MkMaxTreeNode<O> parent, int index, AbstractMTree<O, MkMaxTreeNode<O>, MkMaxEntry, ?> mTree) {
    super.integrityCheckParameters(parentEntry, parent, index, mTree);
    // test if knn distance is correctly set
    MkMaxEntry entry = parent.getEntry(index);
    double knnDistance = kNNDistance();
    if(Math.abs(entry.getKnnDistance() - knnDistance) > 0) {
      throw new RuntimeException("Wrong knnDistance in node " + parent.getPageID() + " at index " + index + " (child " + entry + ")" + "\nsoll: " + knnDistance + ",\n ist: " + entry.getKnnDistance());
    }
  }
}
