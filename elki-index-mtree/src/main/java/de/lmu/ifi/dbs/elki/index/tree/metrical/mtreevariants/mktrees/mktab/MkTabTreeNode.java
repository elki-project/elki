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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mktab;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;

/**
 * Represents a node in a MkMax-Tree.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @navhas - contains - MkTabEntry
 * 
 * @param <O> object type
 */
class MkTabTreeNode<O> extends AbstractMTreeNode<O, MkTabTreeNode<O>, MkTabEntry> {
  private static final long serialVersionUID = 2;

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
    super(capacity, isLeaf);
  }

  /**
   * Determines and returns the knn distance of this node as the maximum knn
   * distance of all entries.
   * 
   * @return the knn distance of this node
   */
  protected double[] kNNDistances() {
    int k = getEntry(0).getKnnDistances().length;

    double[] result = new double[k];

    for(int i = 0; i < getNumEntries(); i++) {
      for(int j = 0; j < k; j++) {
        MkTabEntry entry = getEntry(i);
        result[j] = Math.max(result[j], entry.getKnnDistance(j + 1));
      }
    }

    return result;
  }

  @Override
  public boolean adjustEntry(MkTabEntry entry, DBID routingObjectID, double parentDistance, AbstractMTree<O, MkTabTreeNode<O>, MkTabEntry, ?> mTree) {
    super.adjustEntry(entry, routingObjectID, parentDistance, mTree);
    // adjust knn distances
    entry.setKnnDistances(kNNDistances());
    return true; // TODO: improve
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
  protected void integrityCheckParameters(MkTabEntry parentEntry, MkTabTreeNode<O> parent, int index, AbstractMTree<O, MkTabTreeNode<O>, MkTabEntry, ?> mTree) {
    super.integrityCheckParameters(parentEntry, parent, index, mTree);
    // test knn distances
    MkTabEntry entry = parent.getEntry(index);
    double[] knnDistances = kNNDistances();
    if(!Arrays.equals(entry.getKnnDistances(), knnDistances)) {
      String soll = knnDistances.toString();
      String ist = entry.getKnnDistances().toString();
      throw new RuntimeException("Wrong knnDistances in node " + parent.getPageID() + " at index " + index + " (child " + entry + ")" + "\nsoll: " + soll + ",\n ist: " + ist);
    }
  }
}
